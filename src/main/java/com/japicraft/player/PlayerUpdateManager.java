package com.japicraft.player;

import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.CursorProbe;
import com.japicraft.camera.FirstPersonHands;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerTickEvent;

public final class PlayerUpdateManager {
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            CameraAnchor.update(player);
            if (CameraAnchor.isMainCamera(player)) FirstPersonHands.update(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, event -> {
            Player player = event.getPlayer();
            // only react to the rotation the client actually sent, the server-side view gets overwritten by the camera vehicle
            if (!ClientView.record(player, event.getPacket())) {
                return;
            }
            CursorProbe.update(player);
            CameraAnchor.updateTarget(player, ClientView.yaw(player), ClientView.pitch(player));

            CursorProbe.ScreenPosition cursor = CursorProbe.screenPosition(player);
            player.sendActionBar(Component.text(String.format("Clicked sector %s (x=%.3f, y=%.3f)", CursorProbe.sector(player), cursor.x(), cursor.y())));
        });
    }
}
