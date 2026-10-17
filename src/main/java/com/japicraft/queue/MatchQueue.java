package com.japicraft.queue;

import net.minestom.server.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchQueue {
    private static final int ATTACKER_COUNT = 1;
    private static final int DEFENDER_COUNT = 1;
    private static final int REQUIRED_PLAYERS = ATTACKER_COUNT + DEFENDER_COUNT;
    private final ArrayDeque<Player> primary = new ArrayDeque<>();

    public void add(Player player) {
        primary.add(player);
        tryMatch();
    }

    public void tryMatch() {
        if (primary.size() < MatchQueue.REQUIRED_PLAYERS) {
            return;
        }
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < MatchQueue.REQUIRED_PLAYERS; i++) {
            Player current = primary.pop();
            players.add(current);
        }
        Collections.shuffle(players);
        int index = 0;
        for (Player player : players) {
            if (index < DEFENDER_COUNT) {
                // make defender
            } else {
                // make attacker
            }
            index++;
        }
    }
}
