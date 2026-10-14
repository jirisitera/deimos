package com.japicraft.player;

import com.japicraft.Eris;
import com.japicraft.camera.CameraAnchor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public final class PlayerInputManager {
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, event -> {
            Player player = event.getPlayer();
            int cameraIndex = CameraAnchor.nextCamera(player);
            if (cameraIndex < 0) {
                return;
            }
            player.sendPacket(new PostEffectsPacket(CameraAnchor.isMainCamera(player) ? List.of(Eris.HUD) : List.of(Eris.HUD, Eris.CAMERA, Eris.CAMERA_SWITCH)));
            player.scheduler().scheduleTask(() -> {
                // stabilize camera
                player.sendPacket(new PostEffectsPacket(CameraAnchor.isMainCamera(player) ? List.of(Eris.HUD) : List.of(Eris.HUD, Eris.CAMERA)));
            }, TaskSchedule.tick(5), TaskSchedule.stop());
        });
    }
}
