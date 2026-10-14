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
    private static final String ITEM_MODEL = Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "ui_probe";
    private static final Color BUFFER_COLOR = new Color(91, 126, 209);
    private final Map<UUID, ProbeState> states = new HashMap<>();

    private static ItemStack createProbe(int yaw, int pitch) {
        return ItemStack.of(Material.ECHO_SHARD).withItemModel(ITEM_MODEL).withCustomModelData(List.of(), List.of(), List.of(), List.of(
            new Color(yaw >>> 8, yaw & 0xFF, pitch >>> 8), new Color(pitch & 0xFF, 91, 126),
            BUFFER_COLOR, BUFFER_COLOR, BUFFER_COLOR, BUFFER_COLOR, BUFFER_COLOR, BUFFER_COLOR
        ));
    }

    private static int encodeAngle(float angle, float fullTurn) {
        return Math.round(Math.clamp(angle, 0.0F, fullTurn) / fullTurn * 65535.0F);
    }

    private static int encodeYaw(float yaw) {
        return encodeAngle(yaw + 180.0F, 360.0F);
    }

    public void update(Player player) {
        int yaw = encodeYaw(player.getPosition().yaw());
        int pitch = encodeAngle(player.getPosition().pitch() + 90.0F, 180.0F);
        int key = (yaw << 16) | pitch;
        ProbeState previous = states.get(player.getUuid());
        if (previous != null && previous.key == key && player.getEquipment(EquipmentSlot.OFF_HAND) == previous.item) {
            return;
        }
        ItemStack item = previous != null && previous.key == key ? previous.item : createProbe(yaw, pitch);
        states.put(player.getUuid(), new ProbeState(key, item));

        if (player.getEquipment(EquipmentSlot.OFF_HAND) != item) {
            player.getInventory().clear();
            player.setEquipment(EquipmentSlot.OFF_HAND, item);
        }
    }

    private record ProbeState(int key, ItemStack item) {
    }
}
