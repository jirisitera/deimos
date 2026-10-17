package com.japicraft.server;

import com.japicraft.camera.CursorProbe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.title.Title;
import net.minestom.server.entity.Player;

import java.awt.geom.Rectangle2D;

public class Button {
    private static final Rectangle2D PLAY = Button.createBoundingBox(0, -0.5, 20, 10);

    public static Rectangle2D createBoundingBox(double centerX, double centerY, int width, int height) {
        double finalCenterX = 0.5F + centerX / 2.0F;
        double finalCenterY = 0.5F - centerY / 2.0F;

        double finalWidth = width / 200.0F;
        double finalHeight = height / 200.0F;

        Rectangle2D rectangle = new Rectangle2D.Double();
        rectangle.setFrameFromCenter(finalCenterX, finalCenterY, finalCenterX + finalWidth, finalCenterY + finalHeight);
        return rectangle;
    }

    public static void checkForHovers(Player player, CursorProbe.ScreenPosition cursor) {
        float x = cursor.x();
        float y = cursor.y();

        if (Button.isHovered(x, y, PLAY)) {
            //player.showTitle(Title.title(Component.text("|■=b-1").shadowColor(ShadowColor.none()).font(Font.BUTTON), Component.empty(), 0, 1000, 0));
        } else {
            //player.showTitle(Title.title(Component.text("|■=a-1").shadowColor(ShadowColor.none()).font(Font.BUTTON), Component.empty(), 0, 1000, 0));
        }
    }

    private static boolean isHovered(float x, float y, Rectangle2D rectangle) {
        return rectangle.contains(x, y);
    }
}
