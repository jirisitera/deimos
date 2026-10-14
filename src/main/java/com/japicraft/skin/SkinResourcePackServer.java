package com.japicraft.skin;

import com.japicraft.Eris;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.PlayerSocketConnection;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class SkinResourcePackServer {
    public static final int DEFAULT_PORT = 8085;
    private final String bindHost;
    private final int port;
    private final SkinFetcher skinFetcher;
    private final Map<UUID, SkinResourcePack.PackData> hostedPacks = new ConcurrentHashMap<>();
    private HttpServer httpServer;

    public SkinResourcePackServer() {
        this("0.0.0.0", DEFAULT_PORT, new SkinFetcher());
    }

    public SkinResourcePackServer(String bindHost, int port, SkinFetcher skinFetcher) {
        this.bindHost = bindHost;
        this.port = port;
        this.skinFetcher = skinFetcher;
    }

    /**
     * Generates a deterministic UUID for a player's resource pack.
     */
    public static UUID getPackIdForPlayer(UUID playerUuid) {
        return UUID.nameUUIDFromBytes(("eris-skin-" + playerUuid).getBytes());
    }

    /**
     * Starts the embedded HTTP server to serve resource packs.
     */
    public synchronized void start() throws IOException {
        if (httpServer != null) {
            return;
        }
        httpServer = HttpServer.create(new InetSocketAddress(bindHost, port), 0);
        httpServer.createContext("/pack/", this::handlePackRequest);
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
        Eris.LOGGER.info("Resource pack HTTP server started on {}:{}", bindHost, getPort());
    }

    /**
     * Stops the HTTP server.
     */
    public synchronized void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
            Eris.LOGGER.info("Resource pack HTTP server stopped");
        }
    }

    /**
     * Handles HTTP requests for resource pack downloads.
     */
    private void handlePackRequest(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String rawId = path.substring("/pack/".length());
        if (rawId.endsWith(".zip")) {
            rawId = rawId.substring(0, rawId.length() - ".zip".length());
        }

        UUID packId;
        try {
            packId = UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        SkinResourcePack.PackData packData = hostedPacks.get(packId);
        if (packData == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"pack.zip\"");
        exchange.getResponseHeaders().set("ETag", "\"" + packData.sha1Hex() + "\"");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");

        if ("HEAD".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(200, packData.zipBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(packData.zipBytes());
        }
        exchange.close();
    }

    /**
     * Generates, packages, hosts, and sends the skin resource pack to the player.
     * Intended to be called during the configuration phase (AsyncPlayerConfigurationEvent).
     *
     * @param player The player to send the pack to.
     */
    public void sendTo(Player player) {
        try {
            byte[] skinBytes = skinFetcher.fetchSkinPng(player);
            UUID packId = getPackIdForPlayer(player.getUuid());
            SkinResourcePack.PackData packData = SkinResourcePack.create(packId, skinBytes);
            hostedPacks.put(packId, packData);

            String host = resolveHost(player);
            URI packUri = URI.create("http://" + host + ":" + getPort() + "/pack/" + packId + ".zip");

            ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo(packId, packUri, packData.sha1Hex());
            ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .required(true)
                .prompt(Component.text(""))
                .callback((id, status, _) -> Eris.LOGGER.info("Resource pack {} status for player {}: {}", id, player.getUsername(), status))
                .build();

            player.sendResourcePacks(request);
            Eris.LOGGER.info("Sent dynamic skin resource pack to {} at {}", player.getUsername(), packUri);

        } catch (Exception e) {
            Eris.LOGGER.error("Failed to generate and send skin resource pack to {}", player.getUsername(), e);
        }
    }

    /**
     * Resolves the hostname/IP that the client used to connect to the server.
     */
    public String resolveHost(Player player) {
        if (player.getPlayerConnection() instanceof PlayerSocketConnection socketConnection) {
            String serverAddress = socketConnection.getServerAddress();
            if (serverAddress != null && !serverAddress.isBlank()) {
                if (serverAddress.endsWith(".")) {
                    serverAddress = serverAddress.substring(0, serverAddress.length() - 1);
                }
                return serverAddress;
            }
        }
        return "127.0.0.1";
    }

    /**
     * Cleans up stored pack and cached skin when a player leaves.
     */
    public void removePack(UUID playerUuid) {
        UUID packId = getPackIdForPlayer(playerUuid);
        hostedPacks.remove(packId);
        skinFetcher.invalidate(playerUuid);
    }

    public int getPort() {
        return httpServer != null ? httpServer.getAddress().getPort() : port;
    }
}
