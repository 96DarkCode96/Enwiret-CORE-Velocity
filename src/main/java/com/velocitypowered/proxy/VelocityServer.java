/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ProxyVersion;
import com.velocitypowered.darkcode.*;
import com.velocitypowered.proxy.command.VelocityCommandManager;
import com.velocitypowered.proxy.command.builtin.GlistCommand;
import com.velocitypowered.proxy.command.builtin.ServerCommand;
import com.velocitypowered.proxy.command.builtin.ShutdownCommand;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.console.VelocityConsole;
import com.velocitypowered.proxy.event.VelocityEventManager;
import com.velocitypowered.proxy.network.ConnectionManager;
import com.velocitypowered.proxy.plugin.VelocityPluginManager;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.FaviconSerializer;
import com.velocitypowered.proxy.protocol.util.GameProfileSerializer;
import com.velocitypowered.proxy.scheduler.VelocityScheduler;
import com.velocitypowered.proxy.server.ServerMap;
import com.velocitypowered.proxy.util.*;
import com.velocitypowered.proxy.util.bossbar.AdventureBossBarManager;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiter;
import com.velocitypowered.proxy.util.ratelimit.Ratelimiters;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import org.asynchttpclient.AsyncHttpClient;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyPair;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

public class VelocityServer implements ProxyServer, ForwardingAudience {

    public static final Gson GENERAL_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
            .registerTypeHierarchyAdapter(GameProfile.class, GameProfileSerializer.INSTANCE)
            .create();
    private static final Logger logger = LogManager.getLogger(VelocityServer.class);
    private static final Gson PRE_1_16_PING_SERIALIZER = ProtocolUtils
            .getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_15_2)
            .serializer()
            .newBuilder()
            .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
            .create();
    private static final Gson POST_1_16_PING_SERIALIZER = ProtocolUtils
            .getJsonChatSerializer(ProtocolVersion.MINECRAFT_1_16)
            .serializer()
            .newBuilder()
            .registerTypeHierarchyAdapter(Favicon.class, FaviconSerializer.INSTANCE)
            .create();

    private final ConnectionManager cm;
    private final ProxyOptions options;
    private final ServerMap servers;
    private final VelocityCommandManager commandManager;
    private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
    private final VelocityPluginManager pluginManager;
    private final AdventureBossBarManager bossBarManager;
    private final Map<UUID, ConnectedPlayer> connectionsByUuid = new ConcurrentHashMap<>();
    private final Map<String, ConnectedPlayer> connectionsByName = new ConcurrentHashMap<>();
    private final VelocityConsole console;
    private final VelocityEventManager eventManager;
    private final VelocityScheduler scheduler;
    private final VelocityChannelRegistrar channelRegistrar = new VelocityChannelRegistrar();
    private @MonotonicNonNull VelocityConfiguration configuration;
    private @MonotonicNonNull KeyPair serverKeyPair;
    private boolean shutdown = false;
    private @MonotonicNonNull Ratelimiter ipAttemptLimiter;

    VelocityServer(final ProxyOptions options) {
        pluginManager = new VelocityPluginManager(this);
        eventManager = new VelocityEventManager(pluginManager);
        commandManager = new VelocityCommandManager(eventManager);
        scheduler = new VelocityScheduler(pluginManager);
        console = new VelocityConsole(this);
        cm = new ConnectionManager(this);
        servers = new ServerMap(this);
        this.options = options;
        this.bossBarManager = new AdventureBossBarManager();
    }

    public static Gson getPingGsonInstance(ProtocolVersion version) {
        return version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0 ? POST_1_16_PING_SERIALIZER
                : PRE_1_16_PING_SERIALIZER;
    }

    public KeyPair getServerKeyPair() {
        return serverKeyPair;
    }

    @Override
    public VelocityConfiguration getConfiguration() {
        return this.configuration;
    }

    @Override
    public ProxyVersion getVersion() {
        return new ProxyVersion("ENWIRET - CORE", "ENWIRET CORPORATION", "1.0");
    }

    @Override
    public VelocityCommandManager getCommandManager() {
        return commandManager;
    }

    void awaitProxyShutdown() {
        cm.getBossGroup().terminationFuture().syncUninterruptibly();
    }

    @EnsuresNonNull({"serverKeyPair", "servers", "pluginManager", "eventManager", "scheduler",
            "console", "cm", "configuration"})
    void start() {
        logger.info("Booting up {0} {1}...", getVersion().getName(), getVersion().getVersion());

        registerTranslations();

        serverKeyPair = EncryptionUtils.createRsaKeyPair(1024);

        cm.logChannelInformation();

        // Initialize commands first
        commandManager.register("server", new ServerCommand(this));
        commandManager.register("shutdown", new ShutdownCommand(this), "end");
        commandManager.register
        new GlistCommand(this).register();

        this.doStartupConfigLoad();

        for (Map.Entry<String, String> entry : configuration.getServers().entrySet()) {
            servers.register(new ServerInfo(entry.getKey(), AddressUtil.parseAddress(entry.getValue())));
        }

        ipAttemptLimiter = Ratelimiters.createWithMilliseconds(configuration.getLoginRatelimit());
        eventManager.fire(new ProxyInitializeEvent()).join();

        final Integer port = this.options.getPort();
        if (port != null) {
            logger.debug("Overriding bind port to {0} from command line option", port);
            this.cm.bind(new InetSocketAddress(configuration.getBind().getHostString(), port));
        } else {
            this.cm.bind(configuration.getBind());
        }

        if (configuration.isQueryEnabled()) {
            this.cm.queryBind(configuration.getBind().getHostString(), configuration.getQueryPort());
        }
    }

    private void registerTranslations() {
        final TranslationRegistry translationRegistry = TranslationRegistry
                .create(Key.key("velocity", "translations"));
        translationRegistry.defaultLocale(Locale.US);
        try {
            FileSystemUtils.visitResources(VelocityServer.class, path -> {
                logger.info("Loading localizations...");

                try {
                    Files.walk(path).forEach(file -> {
                        if (!Files.isRegularFile(file)) {
                            return;
                        }

                        String filename = com.google.common.io.Files
                                .getNameWithoutExtension(file.getFileName().toString());
                        String localeName = filename.replace("messages_", "")
                                .replace("messages", "")
                                .replace('_', '-');
                        Locale locale;
                        if (localeName.isEmpty()) {
                            locale = Locale.US;
                        } else {
                            locale = Locale.forLanguageTag(localeName);
                        }

                        translationRegistry.registerAll(locale,
                                ResourceBundle.getBundle("com/velocitypowered/proxy/l10n/messages",
                                        locale, UTF8ResourceBundleControl.get()), false);
                        ClosestLocaleMatcher.INSTANCE.registerKnown(locale);
                    });
                } catch (IOException e) {
                    logger.error("Encountered an I/O error whilst loading translations", e);
                }
            }, "com", "velocitypowered", "proxy", "l10n");
        } catch (IOException e) {
            logger.error("Encountered an I/O error whilst loading translations", e);
            return;
        }
        GlobalTranslator.get().addSource(translationRegistry);
    }

    private void doStartupConfigLoad() {
        try {
            Path configPath = Paths.get("velocity.toml");
            configuration = VelocityConfiguration.read(configPath);

            if (!configuration.validate()) {
                logger.error("Your configuration is invalid. Velocity will not start up until the errors "
                        + "are resolved.");
                System.exit(1);
            }

            commandManager.setAnnounceProxyCommands(configuration.isAnnounceProxyCommands());
            MongoDB.init(this);
        } catch (Exception e) {
            logger.error("Unable to read/load/save your velocity.toml. The server will shut down.", e);
            System.exit(1);
        }
    }

    public Bootstrap createBootstrap(@Nullable EventLoopGroup group) {
        return this.cm.createWorker(group);
    }

    public ChannelInitializer<Channel> getBackendChannelInitializer() {
        return this.cm.backendChannelInitializer.get();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public void shutdown(boolean explicitExit, Component reason) {
        if (eventManager == null || pluginManager == null || cm == null || scheduler == null) {
            throw new AssertionError();
        }

        if (!shutdownInProgress.compareAndSet(false, true)) {
            return;
        }

        Runnable shutdownProcess = () -> {
            logger.info("Shutting down the proxy...");

            cm.shutdown();

            ImmutableList<ConnectedPlayer> players = ImmutableList.copyOf(connectionsByUuid.values());
            for (ConnectedPlayer player : players) {
                player.disconnect(reason);
            }

            try {
                boolean timedOut = false;

                try {
                    CompletableFuture<Void> playersTeardownFuture = CompletableFuture.allOf(players.stream()
                            .map(ConnectedPlayer::getTeardownFuture)
                            .toArray((IntFunction<CompletableFuture<Void>[]>) CompletableFuture[]::new));

                    playersTeardownFuture.get(10, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    timedOut = true;
                } catch (ExecutionException e) {
                    timedOut = true;
                    logger.error("Exception while tearing down player connections", e);
                }

                eventManager.fire(new ProxyShutdownEvent()).join();

                timedOut = !eventManager.shutdown() || timedOut;
                timedOut = !scheduler.shutdown() || timedOut;

                if (timedOut) {
                    logger.error("Your plugins took over 10 seconds to shut down.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            shutdown = true;

            if (explicitExit) {
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    System.exit(0);
                    return null;
                });
            }
        };

        if (explicitExit) {
            Thread thread = new Thread(shutdownProcess);
            thread.start();
        } else {
            shutdownProcess.run();
        }
    }

    public void shutdown(boolean explicitExit) {
        shutdown(explicitExit, Component.text("Proxy shutting down."));
    }

    @Override
    public void shutdown(Component reason) {
        shutdown(true, reason);
    }

    @Override
    public void shutdown() {
        shutdown(true);
    }

    public AsyncHttpClient getAsyncHttpClient() {
        return cm.getHttpClient();
    }

    public Ratelimiter getIpAttemptLimiter() {
        return ipAttemptLimiter;
    }

    public boolean canRegisterConnection(ConnectedPlayer connection) {
        String lowerName = connection.getUsername().toLowerCase(Locale.US);
        return !(connectionsByName.containsKey(lowerName)
                || connectionsByUuid.containsKey(connection.getUniqueId()));
    }

    public boolean registerConnection(ConnectedPlayer connection) {
        String lowerName = connection.getUsername().toLowerCase(Locale.US);

        if (!this.configuration.isOnlineModeKickExistingPlayers()) {
            if (connectionsByName.putIfAbsent(lowerName, connection) != null) {
                return false;
            }
            if (connectionsByUuid.putIfAbsent(connection.getUniqueId(), connection) != null) {
                connectionsByName.remove(lowerName, connection);
                return false;
            }
        } else {
            ConnectedPlayer existing = connectionsByUuid.get(connection.getUniqueId());
            if (existing != null) {
                existing.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
            }
            connectionsByName.put(lowerName, connection);
            connectionsByUuid.put(connection.getUniqueId(), connection);
        }
        return true;
    }

    public void unregisterConnection(ConnectedPlayer connection) {
        connectionsByName.remove(connection.getUsername().toLowerCase(Locale.US), connection);
        connectionsByUuid.remove(connection.getUniqueId(), connection);
        bossBarManager.onDisconnect(connection);
    }

    @Override
    public Optional<Player> getPlayer(String username) {
        Preconditions.checkNotNull(username, "username");
        return Optional.ofNullable(connectionsByName.get(username.toLowerCase(Locale.US)));
    }

    @Override
    public Optional<Player> getPlayer(UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid");
        return Optional.ofNullable(connectionsByUuid.get(uuid));
    }

    @Override
    public Collection<Player> matchPlayer(String partialName) {
        Objects.requireNonNull(partialName);

        return getAllPlayers().stream().filter(p -> p.getUsername()
                        .regionMatches(true, 0, partialName, 0, partialName.length()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<RegisteredServer> matchServer(String partialName) {
        Objects.requireNonNull(partialName);

        return getAllServers().stream().filter(s -> s.getServerInfo().getName()
                        .regionMatches(true, 0, partialName, 0, partialName.length()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Player> getAllPlayers() {
        return ImmutableList.copyOf(connectionsByUuid.values());
    }

    @Override
    public int getPlayerCount() {
        return connectionsByUuid.size();
    }

    @Override
    public Optional<RegisteredServer> getServer(String name) {
        return servers.getServer(name);
    }

    @Override
    public Collection<RegisteredServer> getAllServers() {
        return servers.getAllServers();
    }

    @Override
    public RegisteredServer createRawRegisteredServer(ServerInfo server) {
        return servers.createRawRegisteredServer(server);
    }

    @Override
    public RegisteredServer registerServer(ServerInfo server) {
        return servers.register(server);
    }

    @Override
    public void unregisterServer(ServerInfo server) {
        servers.unregister(server);
    }

    @Override
    public VelocityConsole getConsoleCommandSource() {
        return console;
    }

    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public VelocityEventManager getEventManager() {
        return eventManager;
    }

    @Override
    public VelocityScheduler getScheduler() {
        return scheduler;
    }

    @Override
    public VelocityChannelRegistrar getChannelRegistrar() {
        return channelRegistrar;
    }

    @Override
    public InetSocketAddress getBoundAddress() {
        if (configuration == null) {
            throw new IllegalStateException("No configuration");
        }
        return configuration.getBind();
    }

    @Override
    public @NonNull Iterable<? extends Audience> audiences() {
        Collection<Audience> audiences = new ArrayList<>(this.getPlayerCount() + 1);
        audiences.add(this.console);
        audiences.addAll(this.getAllPlayers());
        return audiences;
    }

    public AdventureBossBarManager getBossBarManager() {
        return bossBarManager;
    }

    @Override
    public ResourcePackInfo.Builder createResourcePackBuilder(String url) {
        return new VelocityResourcePackInfo.BuilderImpl(url);
    }

}
