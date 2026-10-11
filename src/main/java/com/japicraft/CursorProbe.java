package com.japicraft;

import net.minestom.server.color.Color;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CursorProbe {
    private static final String ITEM_MODEL = "twentyone:ui_probe";

    private final Map<UUID, ProbeState> states = new HashMap<>();

    private static ItemStack createProbe(int yaw, int pitch) {
        List<net.kyori.adventure.util.RGBLike> colors = List.of(
            rgb(yaw >>> 8, yaw & 0xff, pitch >>> 8),
            rgb(pitch & 0xff, 91, 126),
            rgb(91, 126, 209),
            rgb(91, 126, 209),
            rgb(91, 126, 209),
            rgb(91, 126, 209),
            rgb(91, 126, 209),
            rgb(91, 126, 209)
        );
        return ItemStack.of(Material.PAPER).withItemModel(ITEM_MODEL).withCustomModelData(List.of(), List.of(), List.of(), colors);
    }

    private static Color rgb(int red, int green, int blue) {
        return new Color(red, green, blue);
    }

    private static int encodeAngle(float angle, float fullTurn) {
        return Math.round(Math.clamp(angle, 0.0f, fullTurn) / fullTurn * 65535.0f);
    }

    private static int encodeYaw(float yaw) {
        return encodeAngle(yaw + 180.0f, 360.0f);
    }

    void install(Player player) {
        update(player);
    }

    void update(Player player) {
        int yaw = encodeYaw(player.getPosition().yaw());
        int pitch = encodeAngle(player.getPosition().pitch() + 90.0f, 180.0f);
        int key = (yaw << 16) | pitch;
        ProbeState previous = states.get(player.getUuid());
        if (previous != null && previous.key == key && player.getEquipment(EquipmentSlot.OFF_HAND) == previous.item) {
            return;
        }
        ItemStack item = previous != null && previous.key == key ? previous.item : createProbe(yaw, pitch);
        states.put(player.getUuid(), new ProbeState(key, item));
        if (player.getEquipment(EquipmentSlot.OFF_HAND) != item) {
            player.setEquipment(EquipmentSlot.OFF_HAND, item);
        }
    }

    private record ProbeState(int key, ItemStack item) {
    }
}
