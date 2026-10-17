package com.japicraft.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;

public class Notification {
    private static final String BACKGROUND = "?";

    public static void show(Player player, String text) {
        player.showTitle(Title.title(Component.empty().shadowColor(ShadowColor.none()).append(
                Component.text(Notification.BACKGROUND).font(Font.DEFAULT).color(TextColor.fromHexString("#282C40")),
                Component.text(text).color(TextColor.fromHexString("#020102")),
                Component.text(text).color(TextColor.fromHexString("#FDFEFD")).shadowColor(ShadowColor.shadowColor(-12763587)),
                Component.text(Notification.BACKGROUND).font(Font.DEFAULT).color(TextColor.fromHexString("#020102"))
            ), Component.empty(), 40, 20, 40)
        );
    }
}
