package com.japicraft;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StructureLoader {
    private static final float[][] rotations = new float[][]{
        new float[]{0.0F, 0.4226F, 0.0F, 0.9063F},
        new float[]{0.0F, -0.0175F, 0.0F, 1.0F},
        new float[]{0.0F, -0.3007F, 0.0F, 0.9537F}
    };
    private static final Pos[] positions = new Pos[]{
        new Pos(5.8125, 12.5, 5.790771484375),
        new Pos(4.4375, 12.5, 6.1875),
        new Pos(3.125, 12.5, 5.875)
    };
    private static final ItemStack DEACTIVATED = ItemStack.of(Material.ECHO_SHARD).withItemModel("eris:computer");
    private static final ItemStack ACTIVATED = ItemStack.of(Material.ECHO_SHARD).withItemModel("eris:computer_on");
    private final List<Entity> entities = new ArrayList<>();

    public static void load(ClassLoader loader, String path, InstanceContainer instance) {
        try (InputStream stream = loader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new RuntimeException("Could not find structure file in resources!");
            }
            byte[] data = stream.readAllBytes();
            Schematic structure = SchematicReader.detecting().read(data);
            structure.createBatch().apply(instance, 0, 10, 0, _ -> Eris.LOGGER.atInfo().log(Component.text("Loaded structure " + path + " into the primary instance.")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void spawn(InstanceContainer instance) {
        for (int i = 0; i < 3; i++) {
            int current = i;
            Entity entity = new Entity(EntityType.ITEM_DISPLAY);
            entities.add(entity);

            entity.editEntityMeta(ItemDisplayMeta.class, meta -> {
                meta.setItemStack(StructureLoader.DEACTIVATED);
                meta.setLeftRotation(rotations[current]);
                meta.setHasNoGravity(true);
            });
            entity.setInstance(instance, positions[current]);
        }
    }

    public void activate(Player player) {
        Scheduler scheduler = player.scheduler();
        for (int i = 0; i < 3; i++) {
            int current = i;
            scheduler.scheduleTask(() -> {
                entities.get(current).editEntityMeta(ItemDisplayMeta.class, meta -> {
                    meta.setItemStack(StructureLoader.ACTIVATED);
                });
            }, TaskSchedule.tick(10 * (i + 1)), TaskSchedule.stop());
        }
    }
}
