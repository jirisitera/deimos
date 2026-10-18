package com.japicraft.camera;

import com.japicraft.instance.InstanceManager;
import com.japicraft.player.ClientView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

public final class CameraAnchor {
    public static final float MAX_YAW_OFFSET = 25.0f;
    public static final float MAX_PITCH_OFFSET = 12.5f;
    private static final float SMOOTHING = 0.16f;
    private static final float LEAN_YAW_OFFSET = 90.0f;
    private static final Pos[] CAMERA_POSITIONS = {
        InstanceManager.MAIN_SPAWN.add(0, 1.75, -0.25),
        new Pos(1.4375, 13.925, 1.375, -25.0F, 25.0F),
        new Pos(15.53515625, 13.925, 1.41796875, 25.0F, 25.0F),
        new Pos(1.479736328125, 13.925, 13.46142578125, -35.0F, 25.0F),
        new Pos(15.54638671875, 13.925, 13.45458984375, 35.0F, 25.0F)
    };
    private static final Tag<Entity> anchor = Tag.Transient("cameraAnchor");
    private static final Tag<Float> baseYaw = Tag.Transient("baseYaw");
    private static final Tag<View> view = Tag.Transient("view");
    private static final Tag<Integer> cameraIndex = Tag.Transient("cameraIndex");
    private static final Tag<Task> actionBarTask = Tag.Transient("actionBarTask");

    private static float ease(float current, float target) {
        float delta = unwrapDelta(target - current);
        float progress = SMOOTHING * SMOOTHING * (3.0F - 2.0F * SMOOTHING);
        if (Math.abs(delta) < 0.01F) {
            return target;
        }
        return current + delta * progress;
    }

    private static float unwrapDelta(float delta) {
        while (delta > 180.0F) {
            delta -= 360.0F;
        }
        while (delta < -180.0F) {
            delta += 360.0F;
        }
        return delta;
    }

    private static void sendCameraActionBar(Player player) {
        Component message = Component.text("Cameras: ", NamedTextColor.WHITE);
        for (int index = 1; index < CAMERA_POSITIONS.length; index++) {
            if (index > 1) {
                message = message.append(Component.space());
            }

            boolean selected = index == player.getTag(CameraAnchor.cameraIndex);
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

    private static void showCameraActionBar(Player player) {
        CameraAnchor.sendCameraActionBar(player);
        if (player.hasTag(CameraAnchor.actionBarTask)) {
            return;
        }
        player.setTag(CameraAnchor.actionBarTask, player.scheduler().scheduleTask(
            () -> CameraAnchor.sendCameraActionBar(player),
            TaskSchedule.tick(1),
            TaskSchedule.tick(20)
        ));
    }

    private static void hideCameraActionBar(Player player) {
        Task actionBarTask = player.getTag(CameraAnchor.actionBarTask);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        player.sendActionBar(Component.empty());
        player.removeTag(CameraAnchor.actionBarTask);
    }

    public static void attach(Player player) {
        Entity display = new Entity(EntityType.ITEM_DISPLAY);

        display.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.of(Material.AIR));
            meta.setPosRotInterpolationDuration(3);
            meta.setHasNoGravity(true);
        });

        player.setTag(CameraAnchor.anchor, display);
        player.setTag(CameraAnchor.cameraIndex, 0);

        Pos initialPosition = player.getPosition();

        ClientView.reset(player);
        player.setTag(CameraAnchor.baseYaw, initialPosition.yaw());
        player.setTag(CameraAnchor.view, new View(initialPosition.yaw(), initialPosition.pitch()));

        display.setInstance(player.getInstance(), CAMERA_POSITIONS[0]).thenRun(() -> {
            player.spectate(display);
            display.addPassenger(player);
        });
    }

    public static int nextCamera(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor == null) {
            return -1;
        }
        return setCamera(player, (player.getTag(CameraAnchor.cameraIndex) + 1) % CAMERA_POSITIONS.length);
    }

    public static int setCamera(Player player, int nextIndex) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor == null || nextIndex < 0 || nextIndex >= CAMERA_POSITIONS.length) {
            return -1;
        }
        Pos cameraPosition = CAMERA_POSITIONS[nextIndex];

        player.setTag(CameraAnchor.cameraIndex, nextIndex);
        player.setTag(CameraAnchor.baseYaw, cameraPosition.yaw());
        player.setTag(CameraAnchor.view, new View(cameraPosition.yaw(), cameraPosition.pitch()));

        anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(0));
        anchor.teleport(cameraPosition);

        if (nextIndex == 0) {
            anchor.scheduler().scheduleTask(() -> {
                // set smoothing for the main camera position
                anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(3));
            }, TaskSchedule.tick(3), TaskSchedule.stop());

            hideCameraActionBar(player);
        } else {
            showCameraActionBar(player);
        }
        return nextIndex;
    }

    public static float calculateLeanYaw(Player player) {
        if (!isMainCamera(player)) {
            return 0.0f;
        }
        boolean left = player.inputs().left();
        boolean right = player.inputs().right();
        if (left && !right) {
            return -LEAN_YAW_OFFSET;
        } else if (right && !left) {
            return LEAN_YAW_OFFSET;
        }
        return 0.0f;
    }

    public static void updateInput(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        View view = player.getTag(CameraAnchor.view);
        if (anchor == null || view == null || !isMainCamera(player)) {
            return;
        }

        float baseYaw = player.getTag(CameraAnchor.baseYaw);
        view.targetYaw = unwrapDelta(baseYaw + getYawOffset(player) + calculateLeanYaw(player));
        view.targetPitch = getPitchOffset(player);
    }

    public static void update(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        View view = player.getTag(CameraAnchor.view);
        if (anchor == null || view == null) {
            return;
        }

        if (isMainCamera(player)) {
            updateInput(player);
        }

        float yaw = ease(view.yaw, view.targetYaw);
        float pitch = ease(view.pitch, view.targetPitch);
        if (yaw == view.yaw && pitch == view.pitch) {
            return;
        }
        view.yaw = unwrapDelta(yaw);
        view.pitch = pitch;

        anchor.teleport(anchor.getPosition().withView(view.yaw, view.pitch));
    }

    public static Pos getCameraPosition(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        return anchor == null ? null : anchor.getPosition();
    }

    public static boolean isMainCamera(Player player) {
        if (getCameraPosition(player) == null) {
            return false;
        }
        return player.getTag(CameraAnchor.cameraIndex) == 0;
    }

    public static float getYawOffset(Player player) {
        return Math.clamp(ClientView.mouseYaw(player) / ClientView.MAX_CURSOR_YAW, -1.0f, 1.0f) * MAX_YAW_OFFSET;
    }

    public static float getPitchOffset(Player player) {
        return Math.clamp(ClientView.mousePitch(player) / ClientView.MAX_CURSOR_PITCH, -1.0f, 1.0f) * MAX_PITCH_OFFSET;
    }

    public static void updateTarget(Player player, float inputPitch) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor == null) {
            return;
        }

        int cameraIndex = player.getTag(CameraAnchor.cameraIndex);
        View view = player.getTag(CameraAnchor.view);
        if (view == null) {
            return;
        }
        if (cameraIndex != 0) {
            view.targetYaw = CAMERA_POSITIONS[cameraIndex].yaw();
            view.targetPitch = CAMERA_POSITIONS[cameraIndex].pitch();
            return;
        }

        float baseYaw = player.getTag(CameraAnchor.baseYaw);
        view.targetYaw = unwrapDelta(baseYaw + getYawOffset(player) + calculateLeanYaw(player));
        view.targetPitch = getPitchOffset(player);
    }

    public static void detach(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor != null) {
            anchor.remove();
        }
        Task actionBarTask = player.getTag(CameraAnchor.actionBarTask);
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }
        player.removeTag(CameraAnchor.anchor);
        player.removeTag(CameraAnchor.baseYaw);
        player.removeTag(CameraAnchor.view);
        player.removeTag(CameraAnchor.cameraIndex);
    }

    private static final class View {
        private float yaw;
        private float pitch;
        private float targetYaw;
        private float targetPitch;

        private View(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.targetYaw = yaw;
            this.targetPitch = pitch;
        }
    }
}
