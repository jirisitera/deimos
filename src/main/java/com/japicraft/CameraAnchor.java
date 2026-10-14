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
        Eris.SPAWN.add(0, 1.75, -0.25),
        new Pos(1.4375, 13.925, 1.375, -25.0F, 25.0F),
        new Pos(15.53515625, 13.925, 1.41796875, 25.0F, 25.0F),
        new Pos(1.479736328125, 13.925, 13.46142578125, -35.0F, 25.0F),
        new Pos(15.54638671875, 13.925, 13.45458984375, 35.0F, 25.0F)
    };
    private final Map<UUID, Entity> anchors = new HashMap<>();
    private final Map<UUID, Float> baseYaws = new HashMap<>();
    private final Map<UUID, View> views = new HashMap<>();
    private final Map<UUID, Integer> cameraIndexes = new HashMap<>();
    private final Map<UUID, Task> actionBarTasks = new HashMap<>();

    private static float ease(float current, float target) {
        float progress = SMOOTHING * SMOOTHING * (3.0F - 2.0F * SMOOTHING);
        if (Math.abs(target - current) < 0.01F) {
            return target;
        }
        return current + (target - current) * progress;
    }

    private static float unwrapDelta(float delta) {
        if (delta > 180.0F) {
            return delta - 360.0F;
        }
        if (delta < -180.0F) {
            return delta + 360.0F;
        }
        return delta;
    }

    private static void sendCameraActionBar(Player player, int currentIndex) {
        Component message = Component.text("Cameras: ", NamedTextColor.WHITE);
        for (int index = 1; index < CAMERA_POSITIONS.length; index++) {
            if (index > 1) {
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

    private void showCameraActionBar(Player player, UUID playerId, int currentIndex) {
        sendCameraActionBar(player, currentIndex);
        if (actionBarTasks.containsKey(playerId)) {
            return;
        }
        actionBarTasks.put(playerId, player.scheduler().scheduleTask(
            () -> sendCameraActionBar(player, cameraIndexes.get(playerId)),
            TaskSchedule.tick(1),
            TaskSchedule.tick(20)
        ));
    }

    private void hideCameraActionBar(Player player, UUID playerId) {
        Task actionBarTask = actionBarTasks.remove(playerId);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        player.sendActionBar(Component.empty());
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

        anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(0));

        anchor.teleport(cameraPosition);

        if (nextIndex == 0) {
            anchor.scheduler().scheduleTask(() -> {
                // set smoothing for the main camera position
                anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(3));
            }, TaskSchedule.tick(3), TaskSchedule.stop());

            hideCameraActionBar(player, playerId);
        } else {
            showCameraActionBar(player, playerId, nextIndex);
        }
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

        anchor.teleport(anchor.getPosition().withView(view.yaw, view.pitch));
    }

    Pos getCameraPosition(Player player) {
        Entity anchor = anchors.get(player.getUuid());
        return anchor == null ? null : anchor.getPosition();
    }

    boolean isMainCamera(Player player) {
        if (getCameraPosition(player) == null) {
            return false;
        }
        return cameraIndexes.getOrDefault(player.getUuid(), -1) == 0;
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
