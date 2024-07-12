package com.bilicraft.biliwhitelistvelocity;

import com.bilicraft.biliwhitelistvelocity.Database.BiliDatabase;
import com.bilicraft.biliwhitelistvelocity.commands.*;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.bilicraft.biliwhitelistvelocity.listeners.JoinListener;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.google.inject.Inject;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.enginehub.squirrelid.cache.HashMapCache;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.CacheForwardingService;
import org.enginehub.squirrelid.resolver.HttpRepositoryService;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Plugin(
        id = "biliwhitelist-velocity",
        name = "BiliWhiteList-Velocity",
        version = "1.0-SNAPSHOT",
        authors = {"BigBrother"}
)
@Getter
public class BiliWhiteListVelocity {
    public static final String PREFIX = "BiliWhiteList";
    public static BiliWhiteListVelocity instance;
    private CacheForwardingService resolver;
    private ProfileCache cache;
    private WhiteListManager whiteListManager ;
    private BiliDatabase databaseManager;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public BiliWhiteListVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 加载插件配置
        Config.loadConfig(this);
        // 初始化数据库
        initDatabase();
        // 注册监听器
        server.getEventManager().register(this, new JoinListener(this));
        // 注册指令
        registerCommands();
    }

    private void initDatabase() {
        // 初始化NameMapping
        try{
            this.cache = new SQLiteCache(new File(dataDirectory.toFile(),"cache.db"));
        }catch (Throwable throwable){
            this.cache = new HashMapCache();
        }

        this.resolver = new CacheForwardingService(HttpRepositoryService.forMinecraft(), cache);
        Map<String, Object> config = Config.getConfig();
        this.databaseManager = new BiliDatabase(this,
                (String) config.get("mysql.host"),
                (String) config.get("mysql.user"),
                (String) config.get("mysql.pass"),
                (String) config.get("mysql.database"),
                (Integer) config.get("mysql.port"),
                (Boolean) config.get("mysql.usessl"));
        this.whiteListManager = new WhiteListManager(this);
    }

    /**
     * 注册所有指令
     */
    private void registerCommands(){
        CommandManager commandManager = server.getCommandManager();

        // bcwhitelist
        CommandMeta commandMeta1 = commandManager.metaBuilder("bcwhitelist")
            .plugin(this)
            .build();
        SimpleCommand whiteListCommand = new WhiteListCommand(this);
        commandManager.register(commandMeta1, whiteListCommand);

        // whoinvite
        CommandMeta commandMeta2 = commandManager.metaBuilder("whoinvite")
            .plugin(this)
            .build();
        SimpleCommand whoInviteCommand = new WhoInviteCommand(this);
        commandManager.register(commandMeta2, whoInviteCommand);

        // bcinvite
        CommandMeta commandMeta3 = commandManager.metaBuilder("bcinvite")
            .plugin(this)
            .build();
        SimpleCommand inviteCommand = new InviteCommand(this);
        commandManager.register(commandMeta3, inviteCommand);

        // bcinvitelist
        CommandMeta commandMeta4 = commandManager.metaBuilder("bcinvitelist")
            .plugin(this)
            .build();
        SimpleCommand inviteListCommand = new InviteListCommand(this);
        commandManager.register(commandMeta4, inviteListCommand);

        // bcservermark
        CommandMeta commandMeta5 = commandManager.metaBuilder("bcservermark")
            .plugin(this)
            .build();
        SimpleCommand serverMarkCommand = new ServerMarkCommand(this);
        commandManager.register(commandMeta5, serverMarkCommand);
    }
}
