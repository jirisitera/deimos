package com.japicraft.game;

import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.MapProbe;
import com.japicraft.game.route.EnemyRoutes;
import com.japicraft.game.route.PathNode;
import com.japicraft.game.route.Route;
import com.japicraft.player.PlayerSetupManager;
import com.japicraft.server.Notification;
import com.japicraft.server.PostEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.color.Color;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.packet.server.common.PostEffectsPacket;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Macro-level AI Director.
 * <p>
 * Responsibilities:
 * 1. Pacing (BUILDUP -> PEAK -> RECOVERY).
 * 2. Tracking player camera habits via a per-camera Heatmap so enemies dynamically route around
 * frequently checked cameras.
 * 3. Anti-cheese protection (camera switch stabilization, staring warning, and hallucination/jamming).
 * <p>
 * --- FUTURE EXPANSION IDEAS (Kept out for simplicity) ---
 * - Audio Decoys / Vent Lures: Allow the player to ping a room with sound to pull enemies toward a
 * specific graph node, at the cost of temporary radar blindness.
 * - Coordinated Pincer Attacks: During PEAK phase, hold one enemy 1 step away from the left door
 * until a second enemy reaches the right flank, forcing simultaneous multi-side management.
 * - Per-Camera Power Sabotage: Enemies with zero camera avoidance (e.g., LURKER) could temporarily
 * knock out a camera node's light level when stepping through it.
 */
public final class Director {
    // Anti-cheese tuning constants
    private static final int CAMERA_STABILIZE_TICKS = 8;     // 0.4s delay after switching before camera can ward
    private static final int STARE_WARNING_TICKS = 100;      // 5.0s continuous stare -> warning audio
    private static final int STARE_OVERLOAD_TICKS = 160;     // 8.0s continuous stare -> hallucination + camera jam
    private static final int CAMERA_JAM_DURATION_TICKS = 100; // 5.0s camera disabled after overload
    private final List<ActionBody> bodies = new ArrayList<>();
    private final DoorController doors = new DoorController();
    // Camera usage tracking (1-indexed for cameras 1..4)
    private final float[] cameraHeat = new float[5];
    private int lastActiveCam = 0;
    private int currentCamStareTicks = 0;
    private int cameraStabilizeTicks = 0;
    private int jammedCamera = -1;
    private int cameraJamTicks = 0;
    private int hallucinationRadarTicks = 0;
    private PacingPhase phase = PacingPhase.BUILDUP;
    private int phaseTicks = 0;
    private int matchTicks = 0;
    private int radarUpdateTicks = 0;
    private boolean gameOver = false;
    private boolean cleanedUp = false;

    public static Director createDefault() {
        Director director = new Director();
        // Adding new enemies is a single line referencing any node ID in FACILITY_GRAPH:
        director.spawnEnemy(ActionBodyType.STALKER, "storage_deep_left");
        director.spawnEnemy(ActionBodyType.LURKER, "generator_deep_right");
        return director;
    }

    /**
     * Registers an enemy on the default interconnected facility graph at the given start node ID.
     */
    public Director spawnEnemy(ActionBodyType type, String startNodeId) {
        return spawnEnemy(type, EnemyRoutes.FACILITY_GRAPH, startNodeId);
    }

    /**
     * Registers an enemy on a custom route graph at the given start node ID.
     */
    public Director spawnEnemy(ActionBodyType type, Route route, String startNodeId) {
        bodies.add(new ActionBody(type, route, startNodeId));
        return this;
    }

    public void addBody(ActionBody body) {
        bodies.add(body);
    }

    public List<ActionBody> bodies() {
        return bodies;
    }

    public DoorController doors() {
        return doors;
    }

    public PacingPhase phase() {
        return phase;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public float getCameraHeat(int cameraIndex) {
        if (cameraIndex < 1 || cameraIndex >= cameraHeat.length) return 0.0F;
        return cameraHeat[cameraIndex];
    }

    /**
     * Returns true if the specified camera is currently jammed/overloaded from excessive staring.
     */
    public boolean isCameraJammed(int cameraIndex) {
        return cameraIndex > 0 && jammedCamera == cameraIndex && cameraJamTicks > 0;
    }

    /**
     * Returns true if the camera is stabilized (not mid-switch) and not jammed from staring too long.
     */
    public boolean isCameraFunctional(int cameraIndex) {
        if (cameraIndex <= 0) return false;
        if (cameraStabilizeTicks > 0) return false;
        return !isCameraJammed(cameraIndex);
    }

    public void start(InstanceContainer instance) {
        for (ActionBody body : bodies) {
            body.spawn(instance);
        }
    }

    public void tick(Player player, InstanceContainer instance) {
        if (gameOver) return;

        matchTicks++;
        phaseTicks++;

        // 1. Track player camera usage, heatmap decay, and anti-cheese staring
        updateCameraTracking(player);

        // 2. Tick doors and energy consumption
        doors.tick(player, instance);

        // 3. Manage Macro Tension / Pacing Phase
        updatePacing();

        // 4. Tick Action Bodies
        for (ActionBody body : bodies) {
            body.tick(player, this, doors, instance);
        }

        // 5. Update Security Radar on Monitor 0 every half-second
        radarUpdateTicks++;
        if (radarUpdateTicks >= 10) {
            radarUpdateTicks = 0;
            syncRadar(player);
        }
    }

    private void updateCameraTracking(Player player) {
        // Slowly cool down all camera heat values over time
        for (int i = 1; i < cameraHeat.length; i++) {
            cameraHeat[i] = Math.max(0.0F, cameraHeat[i] * 0.995F);
        }

        if (cameraStabilizeTicks > 0) {
            cameraStabilizeTicks--;
        }
        if (hallucinationRadarTicks > 0) {
            hallucinationRadarTicks--;
        }
        if (cameraJamTicks > 0) {
            cameraJamTicks--;
            if (cameraJamTicks == 0) {
                if (CameraAnchor.getActiveCamera(player) == jammedCamera) {
                    // Restore normal camera shader once jam clears
                    PostEffect.sync(player);
                }
                jammedCamera = -1;
            }
        }

        int activeCam = CameraAnchor.getActiveCamera(player);

        // Detect camera switches
        if (activeCam != lastActiveCam) {
            lastActiveCam = activeCam;
            currentCamStareTicks = 0;
            if (activeCam > 0 && activeCam < cameraHeat.length) {
                // Switching to a camera adds a burst of heat and requires brief feed stabilization
                cameraHeat[activeCam] = Math.min(30.0F, cameraHeat[activeCam] + 1.2F);
                cameraStabilizeTicks = CAMERA_STABILIZE_TICKS;
            }
        }

        // Track continuous staring at a single camera
        if (activeCam > 0 && activeCam < cameraHeat.length) {
            cameraHeat[activeCam] = Math.min(30.0F, cameraHeat[activeCam] + 0.08F);

            if (jammedCamera != activeCam) {
                currentCamStareTicks++;

                if (currentCamStareTicks == STARE_WARNING_TICKS) {
                    // Subtle static hum warning that staring too long is overloading the feed
                    player.playSound(
                        Sound.sound(Key.key("minecraft:entity.enderman.stare"), Sound.Source.AMBIENT, 0.5F, 0.5F),
                        player.getPosition().x(), player.getPosition().y(), player.getPosition().z()
                    );
                } else if (currentCamStareTicks >= STARE_OVERLOAD_TICKS) {
                    triggerCameraHallucination(player, activeCam);
                }
            }
        } else {
            currentCamStareTicks = Math.max(0, currentCamStareTicks - 2);
        }
    }

    private void triggerCameraHallucination(Player player, int activeCam) {
        currentCamStareTicks = 0;
        jammedCamera = activeCam;
        cameraJamTicks = CAMERA_JAM_DURATION_TICKS;
        hallucinationRadarTicks = 80; // Show phantom radar glitch for 4s

        // Play audio hallucination / signal spike
        player.playSound(
            Sound.sound(Key.key("minecraft:entity.warden.listening_angry"), Sound.Source.HOSTILE, 0.85F, 1.6F),
            player.getPosition().x(), player.getPosition().y(), player.getPosition().z()
        );
        // Keep the heavy static distortion active while this camera is jammed
        player.sendPacket(new PostEffectsPacket(PostEffect.resolveActiveEffects(player, PostEffect.CAMERA_SWITCH)));
        Notification.warn(player, "CAMERA 0" + activeCam + " SIGNAL OVERLOAD");
    }

    /**
     * Picks the best next node from the available graph branches based on the player's camera usage heatmap.
     */
    public PathNode chooseNextNode(ActionBody body, List<PathNode> candidates) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.getFirst();

        PathNode best = candidates.getFirst();
        float bestScore = Float.NEGATIVE_INFINITY;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (PathNode candidate : candidates) {
            float score = 0.0F;
            int cam = candidate.cameraZone();
            if (cam > 0 && cam < cameraHeat.length) {
                // Penalize cameras the player checks often, scaled by this enemy's avoidance weight
                score -= cameraHeat[cam] * body.type().cameraAvoidanceWeight();

                // Extra penalty if the player is actively looking at that camera right now
                if (cam == lastActiveCam) {
                    score -= 6.0F * body.type().cameraAvoidanceWeight();
                }
            }

            // Slight penalty if another enemy is already standing on or traveling to that node to encourage spreading out
            boolean occupied = bodies.stream()
                .anyMatch(other -> other != body
                    && other.state() != ActionBody.State.DORMANT
                    && (other.currentNode() == candidate || other.targetNode() == candidate));
            if (occupied) {
                score -= 4.0F;
            }

            // Small random jitter so routes aren't 100% deterministic
            score += random.nextFloat() * 2.5F;

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private void updatePacing() {
        boolean anyAtDoor = bodies.stream().anyMatch(b -> b.state() == ActionBody.State.WAITING_AT_DOOR);

        switch (phase) {
            case BUILDUP -> {
                if (anyAtDoor) {
                    phase = PacingPhase.PEAK;
                    phaseTicks = 0;
                } else if (phaseTicks > 1200) { // after 60s of buildup, naturally elevate threat
                    phase = PacingPhase.PEAK;
                    phaseTicks = 0;
                }
            }
            case PEAK -> {
                // If all door threats were warded, grant reprieve
                if (!anyAtDoor && phaseTicks > 100) {
                    phase = PacingPhase.RECOVERY;
                    phaseTicks = 0;
                }
            }
            case RECOVERY -> {
                if (phaseTicks >= 200) { // 10s breathing room
                    phase = PacingPhase.BUILDUP;
                    phaseTicks = 0;
                }
            }
        }
    }

    public boolean canAdvance(ActionBody requester) {
        if (phase == PacingPhase.RECOVERY) {
            return false;
        }

        // Avoid dogpiling: if an enemy is already at the office door or traveling there, others hold back in the corridor
        boolean anotherAlreadyAtDoor = bodies.stream()
            .anyMatch(b -> b != requester && (b.state() == ActionBody.State.WAITING_AT_DOOR
                || (b.state() == ActionBody.State.RELOCATING && b.targetNode() != null && b.targetNode().isDoorApproach())));

        return !anotherAlreadyAtDoor || !requester.currentNode().isDoorApproach();
    }

    public void onBreach(ActionBody intruder, Player player) {
        if (gameOver) return;
        gameOver = true;

        if (!CameraAnchor.isMainCamera(player)) {
            CameraAnchor.setCamera(player, 0);
            PostEffect.sync(player);
        }

        player.playSound(
            Sound.sound(Key.key("minecraft:entity.warden.roar"), Sound.Source.HOSTILE, 1.0F, 0.8F),
            player.getPosition().x(), player.getPosition().y(), player.getPosition().z()
        );
        Notification.warn(player, "GAME OVER");

        // Allow 2.5s (50 ticks) for the breach jumpscare/roar before resetting to Lobby
        player.scheduler().scheduleTask(() -> {
            if (!player.isOnline()) return;
            resetToLobby(player);
        }, TaskSchedule.tick(50), TaskSchedule.stop());
    }

    public void resetToLobby(Player player) {
        if (cleanedUp) return;
        cleanedUp = true;
        gameOver = true;

        cleanup();
        player.removeTag(PlayerSetupManager.DIRECTOR_TAG);

        if (player.getInstance() instanceof InstanceContainer container) {
            doors.reset(container);
        }

        PostEffect.removeBlackout(player);
        if (!CameraAnchor.isMainCamera(player)) {
            CameraAnchor.setCamera(player, 0);
        }
        PostEffect.sync(player);

        Lobby.enter(player);
    }

    private void syncRadar(Player player) {
        List<MapProbe.Dot> dots = new ArrayList<>();
        // Center office dot (defender location)
        dots.add(new MapProbe.Dot(0.5F, 0.16F, new Color(80, 200, 255))); // Cyan dot at office

        for (ActionBody body : bodies) {
            if (body.state() == ActionBody.State.DORMANT) continue;

            if (body.state() == ActionBody.State.RELOCATING) {
                // Ghostly signal flicker while target has vanished and is traveling
                if ((matchTicks / 3) % 2 == 0) {
                    continue; // Radar blip lost in transit
                }
            }

            Color dotColor = body.type().radarColor();
            if (body.state() == ActionBody.State.OBSERVED) {
                dotColor = new Color(255, 255, 100); // Yellow when frozen on camera
            } else if (body.state() == ActionBody.State.WAITING_AT_DOOR) {
                // Flash bright red at door
                dotColor = (matchTicks / 5) % 2 == 0 ? new Color(255, 30, 30) : new Color(180, 0, 0);
            } else if (body.state() == ActionBody.State.RELOCATING) {
                // Faded blip while relocating
                dotColor = new Color(dotColor.red() / 2, dotColor.green() / 2, dotColor.blue() / 2);
            }

            dots.add(new MapProbe.Dot(body.currentNode().mapX(), body.currentNode().mapY(), dotColor));
        }

        // If the player triggered a staring hallucination, spawn a flickering phantom blip on the radar
        if (hallucinationRadarTicks > 0 && (matchTicks / 5) % 2 == 0) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            dots.add(new MapProbe.Dot(0.25F + rnd.nextFloat() * 0.5F, 0.30F + rnd.nextFloat() * 0.45F, new Color(200, 80, 255)));
        }

        MapProbe.setDots(player, 0, dots);
    }

    public void cleanup() {
        cleanedUp = true;
        for (ActionBody body : bodies) {
            body.despawn();
        }
        bodies.clear();
    }

    public enum PacingPhase {
        BUILDUP,
        PEAK,
        RECOVERY
    }
}
