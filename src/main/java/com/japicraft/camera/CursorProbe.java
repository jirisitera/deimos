package com.japicraft.camera;

import com.japicraft.Eris;
import com.japicraft.player.ClientView;
import net.minestom.server.color.Color;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;

import java.util.List;

public final class CursorProbe {
    public static final int SECTOR_COLUMNS = 5;
    public static final int SECTOR_ROWS = 4;
    private static final String ITEM_MODEL = Eris.NAMESPACE + Eris.NAMESPACE_SEPARATOR + "ui_probe";
    private static final Color BUFFER_COLOR = new Color(91, 126, 209);
    private static final Tag<ProbeState> TAG = Tag.Transient("cursorProbe");

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

    public static void update(Player player) {
        int yaw = encodeYaw(ClientView.yaw(player));
        int pitch = encodeAngle(ClientView.pitch(player) + 90.0F, 180.0F);
        int key = (yaw << 16) | pitch;

        ProbeState previous = player.getTag(CursorProbe.TAG);
        if (previous != null && previous.key == key && player.getEquipment(EquipmentSlot.OFF_HAND) == previous.item) {
            return;
        }
        ItemStack item = previous != null && previous.key == key ? previous.item : createProbe(yaw, pitch);
        player.setTag(CursorProbe.TAG, new ProbeState(key, item));

        if (player.getEquipment(EquipmentSlot.OFF_HAND) != item) {
            player.getInventory().clear();
            player.setEquipment(EquipmentSlot.OFF_HAND, item);
        }
    }

    public static ScreenPosition screenPosition(Player player) {
        ProbeState state = player.getTag(CursorProbe.TAG);
        int yaw;
        int pitch;
        if (state != null) {
            yaw = state.key >>> 16;
            pitch = state.key & 0xFFFF;
        } else {
            yaw = encodeYaw(ClientView.yaw(player));
            pitch = encodeAngle(ClientView.pitch(player) + 90.0F, 180.0F);
        }
        // decode exactly like the shader: angle in degrees -> clamp(angle / 90, -1, 1) -> 0.5 + v * 0.5
        float yawDegrees = yaw / 65535.0F * 360.0F - 180.0F;
        float pitchDegrees = pitch / 65535.0F * 180.0F - 90.0F;
        float x = 0.5F + Math.clamp(yawDegrees / 90.0F, -1.0F, 1.0F) * 0.5F;
        float y = 0.5F + Math.clamp(pitchDegrees / 90.0F, -1.0F, 1.0F) * 0.5F;
        return new ScreenPosition(x, y);
    }

    public static int sector(Player player) {
        ScreenPosition position = screenPosition(player);
        int column = Math.min((int) (position.x() * SECTOR_COLUMNS), SECTOR_COLUMNS - 1);
        int row = Math.min((int) (position.y() * SECTOR_ROWS), SECTOR_ROWS - 1);
        return row * SECTOR_COLUMNS + column;
    }

    public record ScreenPosition(float x, float y) {
    }

    private record ProbeState(int key, ItemStack item) {
    }
}
