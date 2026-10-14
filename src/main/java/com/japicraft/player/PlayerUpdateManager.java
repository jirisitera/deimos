package com.japicraft.player;

import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.CursorProbe;
import com.japicraft.camera.FirstPersonHands;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.event.player.PlayerTickEvent;

public final class PlayerUpdateManager {
    public void register() {

        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            CameraAnchor.update(player);
            if (CameraAnchor.isMainCamera(player)) FirstPersonHands.update(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            CursorProbe.update(player);
            CameraAnchor.updateTarget(player, event.getNewPosition());
        });
    }
}
