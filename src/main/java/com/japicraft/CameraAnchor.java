package com.japicraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class CameraAnchor {
    private static final float MAX_YAW_OFFSET = 90.0f;
    private static final float SMOOTHING = 0.16f;
    private static final Pos[] CAMERA_POSITIONS = {
        new Pos(4.5, 12.75, 4.25, 0, 0),
        new Pos(8, 28, 0, 34, 65),
        new Pos(-16, 28, 0, 23, 43)
    };
    private final Map<UUID, Entity> anchors = new HashMap<>();
    private final Map<UUID, Float> baseYaws = new HashMap<>();
    private final Map<UUID, View> views = new HashMap<>();
    private final Map<UUID, Integer> cameraIndexes = new HashMap<>();
    private final Map<UUID, Task> actionBarTasks = new HashMap<>();

    private static float ease(float current, float target) {
        float progress = SMOOTHING * SMOOTHING * (3.0f - 2.0f * SMOOTHING);
        if (Math.abs(target - current) < 0.01f) {
            return target;
        }
        return current + (target - current) * progress;
    }

    private static float unwrapDelta(float delta) {
        if (delta > 180.0f) {
            return delta - 360.0f;
        }
        if (delta < -180.0f) {
            return delta + 360.0f;
        }
        return delta;
    }

    private static void sendCameraActionBar(Player player, int currentIndex) {
        Component message = Component.text("Cameras: ", NamedTextColor.WHITE);
        for (int index = 0; index < CAMERA_POSITIONS.length; index++) {
            if (index > 0) {
                message = message.append(Component.space());
            }

            boolean selected = index == currentIndex;
            Component cameraId = Component.text(
                selected ? "[" + index + "]" : String.valueOf(index),
                selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY
            );
            if (selected) {
                cameraId = cameraId.decorate(TextDecoration.BOLD);
            }
            message = message.append(cameraId);
        }
        player.sendActionBar(message);
    }

    void attach(Player player, Instance instance) {
        Entity display = new Entity(EntityType.ITEM_DISPLAY);

        display.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.of(Material.AIR));
            meta.setPosRotInterpolationDuration(3);
            meta.setHasNoGravity(true);
        });

        UUID playerId = player.getUuid();
        anchors.put(playerId, display);
        cameraIndexes.put(playerId, 0);

        Pos initialPosition = player.getPosition();
        baseYaws.put(playerId, initialPosition.yaw());
        views.put(playerId, new View(initialPosition.yaw(), initialPosition.pitch()));

        display.setInstance(instance, CAMERA_POSITIONS[0]).thenRun(() -> {
            player.spectate(display);
            display.addPassenger(player);
        });
        sendCameraActionBar(player, 0);
        actionBarTasks.put(playerId, player.scheduler().scheduleTask(
            () -> sendCameraActionBar(player, cameraIndexes.get(playerId)),
            TaskSchedule.tick(1),
            TaskSchedule.tick(20)
        ));
    }

    int nextCamera(Player player) {
        Entity anchor = anchors.get(player.getUuid());
        if (anchor == null) {
            return -1;
        }

        UUID playerId = player.getUuid();
        int nextIndex = (cameraIndexes.get(playerId) + 1) % CAMERA_POSITIONS.length;

        Pos cameraPosition = CAMERA_POSITIONS[nextIndex];

        cameraIndexes.put(playerId, nextIndex);
        baseYaws.put(playerId, cameraPosition.yaw());
        views.put(playerId, new View(cameraPosition.yaw(), cameraPosition.pitch()));

        anchor.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setPosRotInterpolationDuration(0);
        });

        anchor.teleport(cameraPosition);
        anchor.setView(cameraPosition.yaw(), cameraPosition.pitch());

        if (nextIndex == 0) {
            anchor.scheduler().scheduleTask(() -> {
                anchor.editEntityMeta(ItemDisplayMeta.class, meta -> {
                    meta.setPosRotInterpolationDuration(3);
                });
            }, TaskSchedule.tick(3), TaskSchedule.stop());
        }

        sendCameraActionBar(player, nextIndex);
        return nextIndex;
    }

    void update(Player player) {
        Entity anchor = anchors.get(player.getUuid());
        View view = views.get(player.getUuid());
        if (anchor == null || view == null) {
            return;
        }

        float yaw = ease(view.yaw, view.targetYaw);
        float pitch = ease(view.pitch, view.targetPitch);
        if (yaw == view.yaw && pitch == view.pitch) {
            return;
        }
        view.yaw = yaw;
        view.pitch = pitch;

        anchor.setView(view.yaw, view.pitch);
        anchor.teleport(anchor.getPosition().withView(view.yaw, view.pitch));
    }

    void updateTarget(Player player, Pos position) {
        Entity anchor = anchors.get(player.getUuid());
        if (anchor == null) {
            return;
        }

        int cameraIndex = cameraIndexes.get(player.getUuid());
        View view = views.get(player.getUuid());
        if (view == null) {
            return;
        }
        if (cameraIndex != 0) {
            view.targetYaw = CAMERA_POSITIONS[cameraIndex].yaw();
            view.targetPitch = CAMERA_POSITIONS[cameraIndex].pitch();
            return;
        }

        float yawDelta = position.yaw() - view.inputYaw;
        view.inputYaw += unwrapDelta(yawDelta);
        float yawOffset = view.inputYaw - baseYaws.get(player.getUuid());
        view.targetYaw = baseYaws.get(player.getUuid()) + Math.clamp(yawOffset, -MAX_YAW_OFFSET, MAX_YAW_OFFSET);
        view.targetPitch = position.pitch();
    }

    void detach(Player player) {
        UUID playerId = player.getUuid();
        Entity anchor = anchors.remove(playerId);
        baseYaws.remove(playerId);
        views.remove(playerId);
        cameraIndexes.remove(playerId);
        Task actionBarTask = actionBarTasks.remove(playerId);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        if (anchor != null) {
            anchor.remove();
        }
    }

    private static final class View {
        private float yaw;
        private float pitch;
        private float inputYaw;
        private float targetYaw;
        private float targetPitch;

        private View(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.inputYaw = yaw;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
        }
    }
}
