package com.japicraft.camera;

import com.japicraft.Eris;
import net.kyori.adventure.util.RGBLike;
import net.minestom.server.color.Color;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MapProbe {
    public static final int MAX_DOTS = 16;
    private static final float MAX_COORDINATE = 4094.0F;
    private static final String ITEM_MODEL = Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "map_probe";
    private static final Color DEFAULT_COLOR = new Color(255, 255, 255);
    private static final Tag<MapState> TAG = Tag.Transient("mapProbe");

    private MapProbe() {
    }

    public static Color encodePosition(float x, float y) {
        int encodedX = Math.round(Math.clamp(x, 0.0F, 1.0F) * MAX_COORDINATE);
        int encodedY = Math.round(Math.clamp(y, 0.0F, 1.0F) * MAX_COORDINATE);
        int red = (encodedX >>> 4) & 0xFF;
        int green = ((encodedX & 0xF) << 4) | ((encodedY >>> 8) & 0xF);
        int blue = encodedY & 0xFF;
        return new Color(red, green, blue);
    }

    public static ItemStack create(Dot... dots) {
        return create(List.of(dots));
    }

    public static ItemStack create(List<Dot> dots) {
        int count = Math.min(dots.size(), MAX_DOTS);
        List<RGBLike> colors = new ArrayList<>(count * 2);
        for (int i = 0; i < count; i++) {
            Dot dot = dots.get(i);
            colors.add(encodePosition(dot.x(), dot.y()));
            colors.add(dot.color());
        }
        return ItemStack.of(Material.ECHO_SHARD)
            .withItemModel(ITEM_MODEL)
            .withCustomModelData(List.of(), List.of(), List.of(), colors);
    }

    public static void show(Player player, Dot... dots) {
        show(player, List.of(dots));
    }

    public static void show(Player player, List<Dot> dots) {
        List<Dot> clamped = dots.size() > MAX_DOTS ? List.copyOf(dots.subList(0, MAX_DOTS)) : List.copyOf(dots);
        MapState previous = player.getTag(TAG);
        if (previous != null && previous.dots.equals(clamped) && player.getEquipment(EquipmentSlot.MAIN_HAND) == previous.item) {
            return;
        }
        ItemStack item = previous != null && previous.dots.equals(clamped) ? previous.item : create(clamped);
        player.setTag(TAG, new MapState(clamped, item));

        if (player.getEquipment(EquipmentSlot.MAIN_HAND) != item) {
            player.setEquipment(EquipmentSlot.MAIN_HAND, item);
        }
    }

    public static void hide(Player player) {
        player.removeTag(TAG);
        if (!player.getEquipment(EquipmentSlot.MAIN_HAND).isAir()) {
            player.setEquipment(EquipmentSlot.MAIN_HAND, ItemStack.AIR);
        }
    }

    public static boolean isShown(Player player) {
        return player.hasTag(TAG);
    }

    public record Dot(float x, float y, Color color) {
        public Dot {
            Objects.requireNonNull(color, "color");
        }

        public Dot(float x, float y) {
            this(x, y, DEFAULT_COLOR);
        }
    }

    private record MapState(List<Dot> dots, ItemStack item) {
    }
}
