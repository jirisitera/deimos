package com.japicraft;

import com.japicraft.instance.InstanceManager;
import com.japicraft.player.PlayerInputManager;
import com.japicraft.player.PlayerSetupManager;
import com.japicraft.player.PlayerUpdateManager;
import com.japicraft.queue.MatchQueue;
import com.japicraft.skin.SkinResourcePackServer;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;

public final class Eris {
    /* TODO IMPORTANT:

    - multiplayer support (2 groups - defenders and attackers - instance creation based on defenders)
    - 1 person sits at the cameras, 3 players try to get in
    - defender can deter them by closing doors and opening pits - falling into a pit resets them
    - attackers are very slow, and cannot move if the current selected camera can see them (moving will damage them, too much damage will reset them)

    - click any monitor to zoom into it (press SPACE to return)
    - press SPACE to zoom into main monitor, showing power scheme
    - (click any camera on the power scheme map to view it)
    - use A and D to move to side monitors
        - left monitor shows cameras/cheatsheet
        - right monitor shows objective/statistics
    - attacker has multiple hosts, starts game with a map to choose which to possess


    */
    public static final String NAMESPACE = "eris";
    public static final String NAMESPACE_SEPARATOR = ":";
    public static final ComponentLogger LOGGER = ComponentLogger.logger(Eris.class);

    void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        InstanceManager instanceManager = new InstanceManager();
        SkinResourcePackServer packHost = new SkinResourcePackServer();
        MatchQueue queue = new MatchQueue();

        new PlayerSetupManager().register(instanceManager, packHost);
        new PlayerUpdateManager().register();
        new PlayerInputManager().register(instanceManager, queue);
        //new GlobalResourcePack().register();

        server.start("0.0.0.0", 25565);
    }
}
