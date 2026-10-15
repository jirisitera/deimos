package com.japicraft.camera;

import com.japicraft.instance.InstanceManager;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;

public class DefaultCamera {
    private final Entity entity = new Entity(EntityType.ITEM_DISPLAY);

    public DefaultCamera(InstanceContainer instance) {
        entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(ItemStack.AIR);
            meta.setHasNoGravity(true);
        });
        entity.setInstance(instance, InstanceManager.INITIAL_SPAWN);
    }

    public void addPlayer(Player player) {
        player.spectate(entity);
        entity.addPassenger(player);
    }
}
