package com.japicraft.player;

import com.japicraft.camera.Avatar;
import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.CursorProbe;
import com.japicraft.camera.MapProbe;
import com.japicraft.game.Director;
import com.japicraft.server.Button;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.InstanceContainer;

public final class PlayerUpdateManager {
    public static void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            CameraAnchor.update(player);
            if (CameraAnchor.isMainCamera(player)) Avatar.update(player);

            Director director = player.getTag(PlayerSetupManager.DIRECTOR_TAG);
            if (director != null && player.getInstance() instanceof InstanceContainer container) {
                director.tick(player, container);
            }
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerPacketEvent.class, event -> {
            Player player = event.getPlayer();
            // only react to the rotation the client actually sent, the server-side view gets overwritten by the camera vehicle
            if (!ClientView.record(player, event.getPacket())) {
                return;
            }
            CameraAnchor.updateTarget(player);
            if (CameraAnchor.getCameraPosition(player) != null) {
                MapProbe.updateCursor(player);
            } else {
                CursorProbe.update(player);
                Button.checkForHovers(player, CursorProbe.screenPosition(player));
            }
        });
    }
}
