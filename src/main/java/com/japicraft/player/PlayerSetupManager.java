package com.japicraft.player;

import com.japicraft.camera.Avatar;
import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.CursorProbe;
import com.japicraft.camera.MapProbe;
import com.japicraft.instance.InstanceManager;
import com.japicraft.server.PostEffect;
import com.japicraft.skin.SkinResourcePackServer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;

import java.util.List;

public final class PlayerSetupManager {
    public void register(InstanceManager instanceManager, SkinResourcePackServer packHost) {
        // configuration phase
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instanceManager.getDefault());
            Player player = event.getPlayer();
            player.setRespawnPoint(InstanceManager.INITIAL_SPAWN);
            packHost.sendTo(player);
        });
        // join event
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendPacket(new PostEffectsPacket(List.of(PostEffect.HUD)));
            player.setInvisible(true);

            if (event.isFirstSpawn()) {
                CursorProbe.update(player);
                instanceManager.addToDefault(player);
            }
        });
        // leave event
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            CameraAnchor.detach(player);
            Avatar.detach(player);
            MapProbe.detach(player);
            packHost.removePack(player.getUuid());
        });
    }
}
