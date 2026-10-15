package com.japicraft.player;

import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.client.ClientPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerPositionAndRotationPacket;
import net.minestom.server.network.packet.client.play.ClientPlayerRotationPacket;
import net.minestom.server.tag.Tag;

public final class ClientView {
    private static final Tag<View> TAG = Tag.Transient("clientView");

    public static boolean record(Player player, ClientPacket packet) {
        View view = switch (packet) {
            case ClientPlayerRotationPacket rotation -> new View(rotation.yaw(), rotation.pitch());
            case ClientPlayerPositionAndRotationPacket move -> new View(move.position().yaw(), move.position().pitch());
            default -> null;
        };
        if (view == null) {
            return false;
        }
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

    public record View(float yaw, float pitch) {
    }
}
