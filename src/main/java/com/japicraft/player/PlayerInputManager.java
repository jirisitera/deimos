package com.japicraft.player;

import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.MapProbe;
import com.japicraft.server.PostEffect;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerInputEvent;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public final class PlayerInputManager {
    public void register() {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, event -> {
            Player player = event.getPlayer();
            if (CameraAnchor.getCameraPosition(player) == null) {
                return;
            }
            int cameraIndex;
            if (CameraAnchor.isMainCamera(player)) {
                // at the desk: only the camera buttons on the center monitor switch cameras
                int button = MapProbe.hoveredButton(player);
                if (button < 0) {
                    return;
                }
                cameraIndex = CameraAnchor.setCamera(player, button + 1);
            } else {
                // viewing a camera: click to return to the desk
                cameraIndex = CameraAnchor.setCamera(player, 0);
            }
            if (cameraIndex < 0) {
                return;
            }
            player.sendPacket(new PostEffectsPacket(CameraAnchor.isMainCamera(player) ? List.of(PostEffect.HUD) : List.of(PostEffect.HUD, PostEffect.CAMERA, PostEffect.CAMERA_SWITCH)));
            player.scheduler().scheduleTask(() -> {
                // stabilize camera
                player.sendPacket(new PostEffectsPacket(CameraAnchor.isMainCamera(player) ? List.of(PostEffect.HUD) : List.of(PostEffect.HUD, PostEffect.CAMERA)));
            }, TaskSchedule.tick(5), TaskSchedule.stop());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerInputEvent.class, event -> {
            Player player = event.getPlayer();
            if (event.hasPressedForwardKey()) {

            }
            CameraAnchor.updateInput(player);
        });
    }
}
