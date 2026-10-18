package com.japicraft.instance;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;

public class InstanceManager {
    public static final Pos INITIAL_SPAWN = new Pos(0.5, 10.5, 0.5, 0.0F, 0.0F);
    public static final Pos MAIN_SPAWN = new Pos(8.5, 11.0, 4.5, 0.0F, 0.0F);

    public InstanceContainer create(Player player) {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        // generate main structure
        StructureLoader.load(this.getClass().getClassLoader(), "office.nbt", instance);
        StructureLoader loader = new StructureLoader();
        loader.spawnAll(instance);
        loader.assignEntityData(player);
        return instance;
    }
}
