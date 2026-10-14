package com.japicraft.instance;

import com.japicraft.Eris;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;

public class DefaultInstance {
    private final InstanceContainer instance;

    public DefaultInstance() {
        RegistryKey<DimensionType> dimension = MinecraftServer.getDimensionTypeRegistry()
            .register(Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "default", DimensionType.builder()
                .ambientLight(1.0F)
                .build());

        instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);

        instance.setBlock(0, 8, 0, Block.BLACK_CONCRETE, false);
        instance.setBlock(0, 9, 0, Block.WATER, false);
        instance.setBlock(0, 10, 0, Block.WATER, false);
        instance.setBlock(0, 11, 0, Block.WATER, false);
        instance.setBlock(0, 12, 0, Block.BLACK_CONCRETE, false);


        instance.setBlock(1, 9, 0, Block.BLACK_CONCRETE, false);
        instance.setBlock(1, 10, 0, Block.BLACK_CONCRETE, false);
        instance.setBlock(1, 11, 0, Block.BLACK_CONCRETE, false);

        instance.setBlock(-1, 9, 0, Block.BLACK_CONCRETE, false);
        instance.setBlock(-1, 10, 0, Block.BLACK_CONCRETE, false);
        instance.setBlock(-1, 11, 0, Block.BLACK_CONCRETE, false);

        instance.setBlock(0, 9, 1, Block.BLACK_CONCRETE, false);
        instance.setBlock(0, 10, 1, Block.BLACK_CONCRETE, false);
        instance.setBlock(0, 11, 1, Block.BLACK_CONCRETE, false);

        instance.setBlock(0, 9, -1, Block.BLACK_CONCRETE, false);
        instance.setBlock(0, 10, -1, Block.BLACK_CONCRETE, false);
        instance.setBlock(0, 11, -1, Block.BLACK_CONCRETE, false);
    }

    public InstanceContainer get() {
        return instance;
    }
}
