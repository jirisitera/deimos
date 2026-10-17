package com.japicraft.player;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionAndRotationPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerRotationPacket;
import net.minestom.server.tag.Tag;

public final class ClientView {
    private static final Tag<View> TAG = Tag.Transient("clientView");
    private static final Tag<Float> MOUSE_YAW = Tag.Transient("clientMouseYaw");

    private static float wrapDegrees(float angle) {
        float wrapped = angle % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static boolean record(Player player, ClientPacket packet) {
        View view = switch (packet) {
            case ClientPlayerRotationPacket rotation -> new View(rotation.yaw(), rotation.pitch());
            case ClientPlayerPositionAndRotationPacket move -> new View(move.position().yaw(), move.position().pitch());
            default -> null;
        };
        if (view == null) {
            return false;
        }
        View previous = player.getTag(TAG);
        Float mouseYaw = player.getTag(MOUSE_YAW);
        // accumulate per-packet deltas, so the value stays continuous even if the client wraps its yaw
        float nextMouseYaw = previous == null || mouseYaw == null
            ? wrapDegrees(view.yaw())
            : mouseYaw + wrapDegrees(view.yaw() - previous.yaw());
        player.setTag(MOUSE_YAW, nextMouseYaw);
        player.setTag(TAG, view);
        return true;
    }

    public static float yaw(Player player) {
        View view = player.getTag(TAG);
        return view == null ? player.getPosition().yaw() : view.yaw();
    }

    public static float pitch(Player player) {
        View view = player.getTag(TAG);
        return view == null ? player.getPosition().pitch() : view.pitch();
    }

    public static float mouseYaw(Player player) {
        Float mouseYaw = player.getTag(MOUSE_YAW);
        return mouseYaw == null ? wrapDegrees(yaw(player)) : mouseYaw;
    }

    public record View(float yaw, float pitch) {
    }
}
