package com.japicraft;

import com.japicraft.server.GlobalResourcePack;
import com.japicraft.skin.SkinResourcePackServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.*;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;

public final class Eris {
    /* TODO IMPORTANT:

    - resource pack sending
    - multiplayer support (2 groups - defenders and attackers - instance creation based on defenders)
    - 1 person sits at the cameras, 3 players try to get in
    - defender can deter them by closing doors and opening pits - falling into a pit resets them
    - attackers are very slow, and cannot move if the current selected camera can see them (moving will damage them, too much damage will reset them)

    */

    public static final String NAMESPACE = "eris";
    public static final String NAMESPACE_SEPARATOR = ":";
    public static final ComponentLogger LOGGER = ComponentLogger.logger(Eris.class);
    public static final Pos SPAWN = new Pos(8.5, 11.0, 4.5, 0.0F, 0.0F);
    private static final Key HUD = Key.key(NAMESPACE, "hud");
    private static final Key CAMERA = Key.key(NAMESPACE, "security_camera");
    private static final Key CAMERA_SWITCH = Key.key(NAMESPACE, "camera_switch");

    void main() {
        MinecraftServer server = MinecraftServer.init(new Auth.Online());

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);

        StructureLoader.load(this.getClass().getClassLoader(), "office.nbt", instance);
        StructureLoader structureLoader = new StructureLoader();
        structureLoader.spawnAll(instance);

        CursorProbe cursorProbe = new CursorProbe();
        CameraAnchor cameraAnchor = new CameraAnchor();
        FirstPersonHands firstPersonHands = new FirstPersonHands();

        SkinResourcePackServer skinPackServer = new SkinResourcePackServer();
        try {
            skinPackServer.start();
        } catch (Exception e) {
            LOGGER.error("Failed to start skin resource pack server", e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(skinPackServer::stop));

        MinecraftServer.getGlobalEventHandler().addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            Player player = event.getPlayer();
            player.setRespawnPoint(SPAWN);
            cursorProbe.update(player);
            skinPackServer.sendTo(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
            player.sendPacket(new PostEffectsPacket(List.of(Eris.HUD)));
            player.setInvisible(true);
            cameraAnchor.attach(player, instance);
            firstPersonHands.attach(player, instance);
            structureLoader.activate(player);
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerTickEvent.class, event -> {
            Player player = event.getPlayer();
            cameraAnchor.update(player);
            if (cameraAnchor.isMainCamera(player)) firstPersonHands.update(player);
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerMoveEvent.class, event -> {
            Player player = event.getPlayer();
            cursorProbe.update(player);
            cameraAnchor.updateTarget(player, event.getNewPosition());
        });
        MinecraftServer.getGlobalEventHandler().addListener(PlayerHandAnimationEvent.class, event -> {
            Player player = event.getPlayer();
            int cameraIndex = cameraAnchor.nextCamera(player);
            if (cameraIndex < 0) {
                return;
            }
            player.sendPacket(new PostEffectsPacket(cameraAnchor.isMainCamera(player) ? List.of(Eris.HUD) : List.of(Eris.HUD, Eris.CAMERA, Eris.CAMERA_SWITCH)));
            player.scheduler().scheduleTask(() -> {
                // stabilize camera
                player.sendPacket(new PostEffectsPacket(cameraAnchor.isMainCamera(player) ? List.of(Eris.HUD) : List.of(Eris.HUD, Eris.CAMERA)));
            }, TaskSchedule.tick(5), TaskSchedule.stop());
        });

        MinecraftServer.getGlobalEventHandler().addListener(PlayerDisconnectEvent.class, event -> {
            Player player = event.getPlayer();
            cameraAnchor.detach(player);
            firstPersonHands.detach(player);
            skinPackServer.removePack(player.getUuid());
        });

        GlobalResourcePack resourcePack = new GlobalResourcePack();
        resourcePack.register();

        server.start("0.0.0.0", 25565);
    }
}
