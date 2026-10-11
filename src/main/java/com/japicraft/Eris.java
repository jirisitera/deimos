package com.japicraft;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;

import java.util.List;

public final class Eris {
    public static final ComponentLogger LOGGER = ComponentLogger.logger(Eris.class);
    public static final Pos SPAWN = new Pos(8.5, 11.0, 4.5, 0.0F, 0.0F);
    void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);

        StructureLoader.load(this.getClass().getClassLoader(), "office.nbt", instance);
        StructureLoader structureLoader = new StructureLoader();
        structureLoader.spawnAll(instance);

        CursorProbe cursorProbe = new CursorProbe();
        CameraAnchor cameraAnchor = new CameraAnchor();
        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);

            Player player = event.getPlayer();
            player.setRespawnPoint(SPAWN);
            cursorProbe.install(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendPacket(new PostEffectsPacket(List.of(Key.key("twentyone:hud"))));
            player.setInvisible(true);
            cameraAnchor.attach(player, instance);
            structureLoader.activate(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            cameraAnchor.update(event.getPlayer());
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            cursorProbe.update(player);
            cameraAnchor.updateTarget(player, event.getNewPosition());
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, event -> {
            int cameraIndex = cameraAnchor.nextCamera(event.getPlayer());
            if (cameraIndex < 0) {
                return;
            }
            List<Key> effects = cameraIndex == 0 ? List.of(Key.key("twentyone:hud"))
                : List.of(Key.key("twentyone:hud"), Key.key("twentyone:security_camera"));
            event.getPlayer().sendPacket(new PostEffectsPacket(effects));
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> cameraAnchor.detach(event.getPlayer()));

        server.start("0.0.0.0", 25565);
    }
}
