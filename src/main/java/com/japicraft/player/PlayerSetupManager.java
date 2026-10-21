package com.japicraft.player;

import com.japicraft.camera.Avatar;
import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.MapProbe;
import com.japicraft.instance.InstanceManager;
import com.japicraft.instance.StructureLoader;
import com.japicraft.server.Notification;
import com.japicraft.server.PostEffect;
import com.japicraft.skin.SkinResourcePackServer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;

import com.japicraft.enemy.Director;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.tag.Tag;

import java.util.List;

public final class PlayerSetupManager {
    public static final Tag<Director> DIRECTOR_TAG = Tag.Transient("enemyDirector");

    public void register(InstanceManager instanceManager, SkinResourcePackServer packHost) {
        // configuration phase
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(instanceManager.create(player));
            player.setRespawnPoint(InstanceManager.INITIAL_SPAWN);
            packHost.sendTo(player);
        });
        // join event
        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendPacket(new PostEffectsPacket(List.of(PostEffect.HUD)));
            player.setInvisible(true);
            //CursorProbe.update(player);


            CameraAnchor.attach(player);
            Avatar.attach(player);
            StructureLoader.activate(player);

            Director director = Director.createDefault();
            player.setTag(DIRECTOR_TAG, director);
            if (player.getInstance() instanceof InstanceContainer container) {
                director.start(container);
            }

            Notification.info(player, "Welcome to Eris!");
        });
        // leave event
        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            Director director = player.getTag(DIRECTOR_TAG);
            if (director != null) {
                director.cleanup();
                player.removeTag(DIRECTOR_TAG);
            }
            CameraAnchor.detach(player);
            Avatar.detach(player);
            MapProbe.detach(player);
            packHost.removePack(player.getUuid());
        });
    }
}
