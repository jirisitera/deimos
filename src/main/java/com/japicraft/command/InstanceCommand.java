package com.japicraft.command;

import com.japicraft.camera.Avatar;
import com.japicraft.camera.CameraAnchor;
import com.japicraft.camera.MapProbe;
import com.japicraft.game.Director;
import com.japicraft.game.Lobby;
import com.japicraft.instance.InstanceManager;
import com.japicraft.player.PlayerSetupManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.*;

public class InstanceCommand extends Command {
    public InstanceCommand() {
        super("instance", "instances");
        setCondition((sender, _) -> !(sender instanceof Player player) || player.getPermissionLevel() >= 4);

        var tpLiteral = ArgumentType.Literal("tp");
        var teleportLiteral = ArgumentType.Literal("teleport");
        var targetArg = ArgumentType.Word("target");
        var playerArg = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        targetArg.setSuggestionCallback((_, _, suggestion) -> {
            for (Instance instance : MinecraftServer.getInstanceManager().getInstances()) {
                String uuid = instance.getUuid().toString();
                int players = instance.getPlayers().size();
                suggestion.addEntry(new SuggestionEntry(uuid, Component.text(players + " player" + (players == 1 ? "" : "s"))));
            }
            for (Player onlinePlayer : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
                suggestion.addEntry(new SuggestionEntry(onlinePlayer.getUsername(), Component.text("Player's instance")));
            }
        });

        // /instance (list all instances)
        setDefaultExecutor((sender, _) -> {
            Collection<Instance> instances = MinecraftServer.getInstanceManager().getInstances();

            sender.sendMessage(Component.text("Loaded instances: " + instances.size(), NamedTextColor.GRAY));
            for (Instance instance : instances) {
                String uuid = instance.getUuid().toString();
                long entityCount = instance.getEntities().stream().filter(entity -> !entity.getEntityType().equals(EntityType.PLAYER)).count();
                int playerCount = instance.getPlayers().size();
                boolean isCurrent = sender instanceof Player p && p.getInstance() == instance;

                Component line = Component.text("- ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(uuid, isCurrent ? NamedTextColor.GREEN : NamedTextColor.AQUA))
                    .append(Component.text(String.format(" (entities: %d, players: %d)", entityCount, playerCount), NamedTextColor.GRAY));

                if (isCurrent) {
                    line = line.append(Component.text(" [current]", NamedTextColor.GREEN));
                } else if (sender instanceof Player) {
                    line = line.append(Component.space())
                        .append(Component.text("[Teleport]", NamedTextColor.GOLD)
                            .hoverEvent(HoverEvent.showText(Component.text("Click to teleport to this instance", NamedTextColor.YELLOW)))
                            .clickEvent(ClickEvent.runCommand("/instance tp " + uuid)));
                }
                sender.sendMessage(line);
            }
        });

        // /instance tp <target>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a player: /instance tp <player> <target>", NamedTextColor.RED));
                return;
            }
            handleTeleport(sender, player, context.get(targetArg));
        }, tpLiteral, targetArg);

        // /instance teleport <target>
        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Console must specify a player: /instance teleport <player> <target>", NamedTextColor.RED));
                return;
            }
            handleTeleport(sender, player, context.get(targetArg));
        }, teleportLiteral, targetArg);

        // /instance tp <player> <target>
        addSyntax((sender, context) -> {
            EntityFinder finder = context.get(playerArg);
            Player targetPlayer = finder.findFirstPlayer(sender);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            handleTeleport(sender, targetPlayer, context.get(targetArg));
        }, tpLiteral, playerArg, targetArg);

        // /instance teleport <player> <target>
        addSyntax((sender, context) -> {
            EntityFinder finder = context.get(playerArg);
            Player targetPlayer = finder.findFirstPlayer(sender);
            if (targetPlayer == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
            handleTeleport(sender, targetPlayer, context.get(targetArg));
        }, teleportLiteral, playerArg, targetArg);

        // /instance tp (missing target)
        addSyntax((sender, _) -> sender.sendMessage(Component.text("Usage: /instance tp <instance | player> or /instance tp <player> <instance>", NamedTextColor.RED)), tpLiteral);
        addSyntax((sender, _) -> sender.sendMessage(Component.text("Usage: /instance teleport <instance | player> or /instance teleport <player> <instance>", NamedTextColor.RED)), teleportLiteral);
    }

    public static FindResult resolveInstance(String input) {
        if (input == null || input.isBlank()) {
            return new FindResult.NotFound();
        }

        net.minestom.server.instance.InstanceManager manager = MinecraftServer.getInstanceManager();
        Collection<Instance> instances = manager.getInstances();

        // 1. Exact UUID match
        try {
            UUID uuid = UUID.fromString(input.trim());
            Instance byUuid = manager.getInstance(uuid);
            if (byUuid != null) {
                return new FindResult.Found(byUuid);
            }
        } catch (IllegalArgumentException ignored) {
        }

        // 2. UUID prefix match (case-insensitive)
        String lower = input.trim().toLowerCase(Locale.ROOT);
        List<Instance> prefixMatches = new ArrayList<>();
        for (Instance inst : instances) {
            String idStr = inst.getUuid().toString().toLowerCase(Locale.ROOT);
            if (idStr.startsWith(lower)) {
                prefixMatches.add(inst);
            }
        }
        if (prefixMatches.size() == 1) {
            return new FindResult.Found(prefixMatches.getFirst());
        } else if (prefixMatches.size() > 1) {
            return new FindResult.Ambiguous(prefixMatches);
        }

        // 3. Online player match (target player's current instance)
        Player targetPlayer = MinecraftServer.getConnectionManager().findOnlinePlayer(input.trim());
        if (targetPlayer != null && targetPlayer.getInstance() != null) {
            return new FindResult.Found(targetPlayer.getInstance());
        }

        return new FindResult.NotFound();
    }

    public static void handleTeleport(CommandSender sender, Player player, String targetInput) {
        FindResult result = resolveInstance(targetInput);
        if (result instanceof FindResult.Found(Instance instance)) {
            teleportPlayerToInstance(sender, player, instance);
        } else if (result instanceof FindResult.Ambiguous(List<Instance> matches)) {
            sender.sendMessage(Component.text("Multiple instances matched '" + targetInput + "':", NamedTextColor.RED));
            for (Instance match : matches) {
                sender.sendMessage(Component.text(" - " + match.getUuid(), NamedTextColor.GRAY));
            }
        } else {
            sender.sendMessage(Component.text("Instance not found: " + targetInput, NamedTextColor.RED));
        }
    }

    public static void teleportPlayerToInstance(CommandSender sender, Player player, Instance targetInstance) {
        if (player.getInstance() == targetInstance) {
            sender.sendMessage(Component.text(
                sender == player ? "You are already in this instance!" : player.getUsername() + " is already in this instance!",
                NamedTextColor.RED
            ));
            return;
        }

        Director director = player.getTag(PlayerSetupManager.DIRECTOR_TAG);
        if (director != null) {
            director.cleanup();
            player.removeTag(PlayerSetupManager.DIRECTOR_TAG);
        }
        Lobby.leave(player);
        CameraAnchor.detach(player);
        Avatar.detach(player);
        MapProbe.detach(player);

        sender.sendMessage(Component.text(
            "Teleporting " + (sender == player ? "to" : player.getUsername() + " to") + " instance " + targetInstance.getUuid() + "...",
            NamedTextColor.GRAY
        ));

        player.setInstance(targetInstance, InstanceManager.INITIAL_SPAWN).thenRun(() -> {
            player.sendMessage(Component.text("Teleported to instance: " + targetInstance.getUuid(), NamedTextColor.GREEN));
        });
    }

    public sealed interface FindResult {
        record Found(Instance instance) implements FindResult {
        }

        record Ambiguous(List<Instance> matches) implements FindResult {
        }

        record NotFound() implements FindResult {
        }
    }
}
