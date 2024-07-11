package com.bilicraft.biliwhitelistvelocity;

import com.bilicraft.biliwhitelistvelocity.Database.BiliDatabase;
import com.bilicraft.biliwhitelistvelocity.commands.CommandBuilder;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.google.inject.Inject;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.resolver.CacheForwardingService;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "biliwhitelist-velocity",
        name = "BiliWhiteList-Velocity",
        version = "1.0-SNAPSHOT",
        authors = {"BigBrother"}
)
public class BiliWhiteListVelocity {
    public static final String PREFIX = "BiliWhiteList";
    public static BiliWhiteListVelocity instance;
    @Getter
    private CacheForwardingService resolver;
    private ProfileCache cache;
    @Getter
    private WhiteListManager whiteListManager ;
    @Getter
    private BiliDatabase databaseManager;
    @Getter
    private final ProxyServer server;
    @Getter
    private final Logger logger;
    @Getter
    private final Path dataDirectory;

    @Inject
    public BiliWhiteListVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 加载插件
        Config.loadConfig(this);
        // 注册监听器
        server.getEventManager().register(this, null);
        // 初始化指令
        LiteralCommandNode<CommandSource> rootNode = LiteralArgumentBuilder.<CommandSource>literal("bcwhitelist").build();
        // 注册指令
        CommandBuilder.register(this);
    }
}
