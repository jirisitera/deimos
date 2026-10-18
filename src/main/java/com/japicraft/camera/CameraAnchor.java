package com.japicraft.camera;

import com.japicraft.instance.InstanceManager;
import com.japicraft.player.ClientView;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;

public final class CameraAnchor {
    public static final float MAX_YAW_OFFSET = 8.0f;
    public static final float MAX_PITCH_OFFSET = 8.0f;
    public static final double MONITOR_ZOOM = 0.22;
    private static final float SMOOTHING = 0.16f;
    private static final float LEAN_YAW_OFFSET = 90.0f;
    private static final Pos[] CAMERA_POSITIONS = {
        InstanceManager.MAIN_SPAWN.add(0, 1.75, -0.25),
        new Pos(1.4375, 13.925, 1.375, -25.0F, 25.0F),
        new Pos(15.53515625, 13.925, 1.41796875, 25.0F, 25.0F),
        new Pos(1.479736328125, 13.925, 13.46142578125, -35.0F, 25.0F),
        new Pos(15.54638671875, 13.925, 13.45458984375, 35.0F, 25.0F)
    };
    private static final Pos[] MONITOR_TARGETS = {
        new Pos(9.8125, 12.6, 5.790771484375),
        new Pos(8.4375, 12.6, 6.1875),
        new Pos(7.125, 12.6, 5.875)
    };
    private static final Tag<Entity> anchor = Tag.Transient("cameraAnchor");
    private static final Tag<Float> baseYaw = Tag.Transient("baseYaw");
    private static final Tag<View> view = Tag.Transient("view");
    private static final Tag<Integer> cameraIndex = Tag.Transient("cameraIndex");

    private static float ease(float current, float target) {
        float delta = unwrapDelta(target - current);
        float progress = SMOOTHING * SMOOTHING * (3.0F - 2.0F * SMOOTHING);
        if (Math.abs(delta) < 0.01F) {
            return target;
        }
        return current + delta * progress;
    }

    private static double easeCoord(double current, double target) {
        double delta = target - current;
        double progress = SMOOTHING * SMOOTHING * (3.0 - 2.0 * SMOOTHING);
        if (Math.abs(delta) < 0.001) {
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

    private static float monitorVirtualX(Player player) {
        return Math.clamp(
            (ClientView.mouseYaw(player) + ClientView.MAX_CURSOR_YAW) / (ClientView.MAX_CURSOR_YAW * 2.0F) * MONITOR_TARGETS.length,
            0.0F,
            MONITOR_TARGETS.length - 0.0001F
        );
    }

    private static int activeMonitor(Player player) {
        return (int) Math.floor(monitorVirtualX(player));
    }

    private static float monitorCenterYaw(int monitorIndex) {
        Pos base = CAMERA_POSITIONS[0];
        Pos monitor = MONITOR_TARGETS[monitorIndex];
        double dx = monitor.x() - base.x();
        double dz = monitor.z() - base.z();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static Pos calculateMainTargetPos(Player player) {
        Pos base = CAMERA_POSITIONS[0];
        if (calculateLeanYaw(player) != 0.0f) {
            return base;
        }
        Pos monitor = MONITOR_TARGETS[activeMonitor(player)];
        return new Pos(
            base.x() + (monitor.x() - base.x()) * MONITOR_ZOOM,
            base.y() + (monitor.y() - base.y()) * MONITOR_ZOOM,
            base.z() + (monitor.z() - base.z()) * MONITOR_ZOOM
        );
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
        Pos startPos = calculateMainTargetPos(player).withView(
            unwrapDelta(initialPosition.yaw() + getYawOffset(player)),
            getPitchOffset(player)
        );
        player.setTag(CameraAnchor.view, new View(startPos));

        display.setInstance(player.getInstance(), startPos).thenRun(() -> {
            player.spectate(display);
            display.addPassenger(player);
        });
    }

    public static int setCamera(Player player, int nextIndex) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor == null || nextIndex < 0 || nextIndex >= CAMERA_POSITIONS.length) {
            return -1;
        }
        Pos cameraPosition = CAMERA_POSITIONS[nextIndex];

        player.setTag(CameraAnchor.cameraIndex, nextIndex);
        player.setTag(CameraAnchor.baseYaw, cameraPosition.yaw());

        float leanYaw = calculateLeanYaw(player);
        float initialYaw = nextIndex == 0 ? unwrapDelta(cameraPosition.yaw() + (leanYaw != 0.0f ? leanYaw : getYawOffset(player))) : cameraPosition.yaw();
        float initialPitch = nextIndex == 0 ? getPitchOffset(player) : cameraPosition.pitch();
        Pos initialPos = (nextIndex == 0 ? calculateMainTargetPos(player) : cameraPosition).withView(initialYaw, initialPitch);
        player.setTag(CameraAnchor.view, new View(initialPos));

        anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(0));
        anchor.teleport(initialPos);

        if (nextIndex == 0) {
            anchor.scheduler().scheduleTask(() -> {
                // set smoothing for the main camera position
                anchor.editEntityMeta(ItemDisplayMeta.class, meta -> meta.setPosRotInterpolationDuration(3));
            }, TaskSchedule.tick(3), TaskSchedule.stop());

            //hideCameraActionBar(player);
        } else {
            //showCameraActionBar(player);
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
        float leanYaw = calculateLeanYaw(player);
        view.targetYaw = unwrapDelta(baseYaw + (leanYaw != 0.0f ? leanYaw : getYawOffset(player)));
        view.targetPitch = getPitchOffset(player);
        Pos targetPos = calculateMainTargetPos(player);
        view.targetX = targetPos.x();
        view.targetY = targetPos.y();
        view.targetZ = targetPos.z();
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

        double x = easeCoord(view.x, view.targetX);
        double y = easeCoord(view.y, view.targetY);
        double z = easeCoord(view.z, view.targetZ);
        float yaw = ease(view.yaw, view.targetYaw);
        float pitch = ease(view.pitch, view.targetPitch);
        if (x == view.x && y == view.y && z == view.z && yaw == view.yaw && pitch == view.pitch) {
            return;
        }
        view.x = x;
        view.y = y;
        view.z = z;
        view.yaw = unwrapDelta(yaw);
        view.pitch = pitch;

        anchor.teleport(new Pos(view.x, view.y, view.z, view.yaw, view.pitch));
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
        float virtualX = monitorVirtualX(player);
        int monitorIndex = (int) Math.floor(virtualX);
        float localCursorX = (virtualX - monitorIndex - 0.5F) * 2.0F; // -1..1 within the active monitor
        return monitorCenterYaw(monitorIndex) + localCursorX * MAX_YAW_OFFSET;
    }

    public static float getPitchOffset(Player player) {
        return Math.clamp(ClientView.mousePitch(player) / ClientView.MAX_CURSOR_PITCH, -1.0f, 1.0f) * MAX_PITCH_OFFSET;
    }

    public static void updateTarget(Player player) {
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
            Pos pos = CAMERA_POSITIONS[cameraIndex];
            view.targetX = pos.x();
            view.targetY = pos.y();
            view.targetZ = pos.z();
            view.targetYaw = pos.yaw();
            view.targetPitch = pos.pitch();
            return;
        }

        updateInput(player);
    }

    public static void detach(Player player) {
        Entity anchor = player.getTag(CameraAnchor.anchor);
        if (anchor != null) {
            anchor.remove();
        }
        player.removeTag(CameraAnchor.anchor);
        player.removeTag(CameraAnchor.baseYaw);
        player.removeTag(CameraAnchor.view);
        player.removeTag(CameraAnchor.cameraIndex);
    }

    private static final class View {
        private double x;
        private double y;
        private double z;
        private float yaw;
        private float pitch;
        private double targetX;
        private double targetY;
        private double targetZ;
        private float targetYaw;
        private float targetPitch;

        private View(Pos pos) {
            this.x = pos.x();
            this.y = pos.y();
            this.z = pos.z();
            this.yaw = pos.yaw();
            this.pitch = pos.pitch();
            this.targetX = pos.x();
            this.targetY = pos.y();
            this.targetZ = pos.z();
            this.targetYaw = pos.yaw();
            this.targetPitch = pos.pitch();
        }
    }
}
