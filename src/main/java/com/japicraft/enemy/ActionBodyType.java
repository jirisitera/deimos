package com.japicraft.enemy;

import net.minestom.server.color.Color;
import net.minestom.server.entity.EntityType;

/**
 * Archetype definition for an ActionBody.
 *
 * @param cameraAvoidanceWeight How strongly this enemy avoids cameras the player checks often (0 = ignores, 2+ = very sneaky).
 * @param transitTicks How many ticks this enemy remains vanished while traveling between nodes (0 = instant vanish-and-reappear).
 */
public record ActionBodyType(
    String id,
    String displayName,
    int baseMoveIntervalTicks,
    int gazeToleranceTicks,
    int doorPatienceTicks,
    int retreatSteps,
    float cameraAvoidanceWeight,
    Color radarColor,
    EntityType entityType,
    int transitTicks
) {
    public ActionBodyType(
        String id,
        String displayName,
        int baseMoveIntervalTicks,
        int gazeToleranceTicks,
        int doorPatienceTicks,
        int retreatSteps,
        float cameraAvoidanceWeight,
        Color radarColor,
        EntityType entityType
    ) {
        this(id, displayName, baseMoveIntervalTicks, gazeToleranceTicks, doorPatienceTicks, retreatSteps, cameraAvoidanceWeight, radarColor, entityType, 20);
    }

    public static final ActionBodyType STALKER = new ActionBodyType(
        "stalker",
        "The Stalker",
        90,                     // moves every 4.5s
        25,                     // 1.25s camera gaze freezes/wards
        100,                    // 5.0s door patience before breach
        2,                      // retreats 2 nodes when warded
        2.0F,                   // strongly avoids heavily watched cameras
        new Color(255, 60, 60), // Red radar dot
        EntityType.ENDERMAN,
        20                      // 1.0s vanish transit duration
    );

    public static final ActionBodyType LURKER = new ActionBodyType(
        "lurker",
        "The Lurker",
        130,                    // slower approach (6.5s)
        40,                     // more resilient to camera gaze (2s)
        80,                     // faster door breach once it arrives (4.0s)
        3,                      // larger retreat when warded
        1.0F,                   // moderate camera avoidance
        new Color(255, 160, 40),// Orange radar dot
        EntityType.ENDERMAN,
        30                      // 1.5s vanish transit duration (heavy stalker)
    );

    public static final ActionBodyType SHADE = new ActionBodyType(
        "shade",
        "The Shade",
        75,                     // fast mover (3.75s)
        15,                     // very fragile to camera gaze (0.75s)
        110,                    // 5.5s door patience
        2,
        3.5F,                   // actively hunts for unwatched blind-spot routes
        new Color(180, 90, 255),// Purple radar dot
        EntityType.ENDERMAN,
        10                      // 0.5s quick ghostly vanish transit
    );
}
