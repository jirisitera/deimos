package com.japicraft.camera;

import com.japicraft.Eris;
import com.japicraft.skin.SkinFetcher;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.Objects;

public final class FirstPersonHands {
    private static final String LEFT_HAND = Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "arm_left";
    private static final String RIGHT_HAND = Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "arm_right";
    private static final double FORWARD_OFFSET = 0.15;
    private static final double SIDE_OFFSET = 0.35;
    private static final double TABLE_HEIGHT_OFFSET = 0.25;
    private static final float RIGHT_YAW_ROTATION = 45.0F;
    private static final float RIGHT_PITCH_ROTATION = 15.0F;
    private static final float[] LEFT_ROTATION = rotationFromDegrees(75.0F, 180.0F, 5.0F);
    private static final float[] RIGHT_ROTATION = rotationFromDegrees(75.0F, 175.0F, 0.0F);
    private static final Tag<Hands> TAG = Tag.Transient("firstPersonHands");

    private static Entity createHand(String hand, boolean slim) {
        Entity entity = new Entity(EntityType.ITEM_DISPLAY);
        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            String model = slim ? hand + "_slim" : hand;
            meta.setItemStack(ItemStack.of(Material.ECHO_SHARD).withItemModel(model));
            meta.setLeftRotation(Objects.equals(hand, LEFT_HAND) ? LEFT_ROTATION : RIGHT_ROTATION);
            meta.setHasNoGravity(true);
            meta.setTransformationInterpolationDuration(3);
        });
        return entity;
    }

    private static Entity createBody() {
        Entity hand = new Entity(EntityType.ITEM_DISPLAY);
        hand.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.of(Material.ECHO_SHARD));
            meta.setHasNoGravity(true);
        });
        return hand;
    }

    private static float[] rotationFromDegrees(float x, float y, float z) {
        double halfX = Math.toRadians(x) * 0.5;
        double halfY = Math.toRadians(y) * 0.5;
        double halfZ = Math.toRadians(z) * 0.5;
        double sinX = Math.sin(halfX);
        double cosX = Math.cos(halfX);
        double sinY = Math.sin(halfY);
        double cosY = Math.cos(halfY);
        double sinZ = Math.sin(halfZ);
        double cosZ = Math.cos(halfZ);
        return new float[]{
            (float) (sinX * cosY * cosZ - cosX * sinY * sinZ),
            (float) (cosX * sinY * cosZ + sinX * cosY * sinZ),
            (float) (cosX * cosY * sinZ - sinX * sinY * cosZ),
            (float) (cosX * cosY * cosZ + sinX * sinY * sinZ)
        };
    }

    private static Pos leftHandPosition(Pos camera) {
        return new Pos(camera.x() + SIDE_OFFSET, camera.y() + TABLE_HEIGHT_OFFSET, camera.z() + FORWARD_OFFSET);
    }

    private static Pos rightHandPosition(Pos camera) {
        return new Pos(camera.x() - SIDE_OFFSET, camera.y() + TABLE_HEIGHT_OFFSET, camera.z() + FORWARD_OFFSET);
    }

    private static float[] rightHandRotation(float yaw, float pitch) {
        float yawOffset = (float) Math.clamp(-angleDelta(yaw) / 180.0 * RIGHT_YAW_ROTATION, -RIGHT_YAW_ROTATION, RIGHT_YAW_ROTATION);
        float pitchOffset = (float) Math.clamp(-pitch / 90.0 * RIGHT_PITCH_ROTATION, -RIGHT_PITCH_ROTATION, RIGHT_PITCH_ROTATION);
        return rotationFromDegrees(75.0F + pitchOffset, 180.0F + yawOffset, 0.0F);
    }

    private static double angleDelta(double angle) {
        return angle - Math.floor((angle + 180.0) / 360.0) * 360.0;
    }

    public static void attach(Player player) {
        boolean slim = SkinFetcher.isSlim(player.getSkin());

        Entity left = createHand(LEFT_HAND, slim);
        Entity right = createHand(RIGHT_HAND, slim);
        Entity body = createBody();

        player.setTag(FirstPersonHands.TAG, new Hands(left, right));

        Pos camera = player.getPosition();
        Instance instance = player.getInstance();

        left.setInstance(instance, leftHandPosition(camera));
        right.setInstance(instance, rightHandPosition(camera));
        body.setInstance(instance, camera);
    }

    public static void update(Player player) {
        Hands pair = player.getTag(FirstPersonHands.TAG);
        if (pair == null) {
            return;
        }
        pair.right.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setLeftRotation(rightHandRotation(player.getPosition().yaw(), player.getPosition().pitch()));
            meta.setTransformationInterpolationStartDelta(0);
        });
    }

    public static void detach(Player player) {
        Hands pair = player.getTag(FirstPersonHands.TAG);
        if (pair == null) {
            return;
        }
        pair.left.remove();
        pair.right.remove();
        player.removeTag(FirstPersonHands.TAG);
    }

    private record Hands(Entity left, Entity right) {
    }
}
