package com.japicraft;

import com.japicraft.player.PlayerInputManager;
import com.japicraft.player.PlayerSetupManager;
import com.japicraft.player.PlayerUpdateManager;
import com.japicraft.server.GlobalResourcePack;
import com.japicraft.server.ServerListManager;
import com.japicraft.skin.SkinResourcePackServer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;

public final class Eris {
    public static final String NAMESPACE = "eris";
    public static final String NAMESPACE_SEPARATOR = ":";
    public static final ComponentLogger LOGGER = ComponentLogger.logger(Eris.class);

    void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        // player managers
        PlayerSetupManager.register(new SkinResourcePackServer());
        PlayerUpdateManager.register();
        PlayerInputManager.register();

        // server managers
        ServerListManager.register();
        GlobalResourcePack.register();

        server.start("0.0.0.0", 25565);
    }
}
