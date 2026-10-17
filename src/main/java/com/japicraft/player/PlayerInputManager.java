package com.japicraft.player;

import com.japicraft.camera.CameraAnchor;
import com.japicraft.instance.InstanceManager;
import com.japicraft.queue.MatchQueue;
import com.japicraft.server.PostEffect;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerHandAnimationEvent;
import net.minestom.server.event.player.PlayerInputEvent;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public final class PlayerInputManager {
    public void register(InstanceManager instanceManager, MatchQueue queue) {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, event -> {
            Player player = event.getPlayer();
            int cameraIndex = CameraAnchor.nextCamera(player);
            if (cameraIndex < 0) {
                //queue.add(player);
                instanceManager.create(player);
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
            CameraAnchor.updateInput(player);
        });
    }
}
