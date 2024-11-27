package com.bilicraft.biliwhitelistvelocity;

import com.bilicraft.biliwhitelistvelocity.Database.BiliDatabase;
import com.bilicraft.biliwhitelistvelocity.commands.*;
import com.bilicraft.biliwhitelistvelocity.common.HttpRepositoryServicePatched;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.bilicraft.biliwhitelistvelocity.listeners.JoinListener;
import com.bilicraft.biliwhitelistvelocity.listeners.LiteBansListener;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.google.inject.Inject;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginManager;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import litebans.api.Events;
import lombok.Getter;
import org.enginehub.squirrelid.cache.HashMapCache;
import org.enginehub.squirrelid.cache.ProfileCache;
import org.enginehub.squirrelid.cache.SQLiteCache;
import org.enginehub.squirrelid.resolver.CacheForwardingService;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

@Plugin(
        id = "biliwhitelist-velocity",
        name = "BiliWhiteList-Velocity",
        version = "1.0-SNAPSHOT",
        description = "Velocity version of BiliWhiteList plugin",
        authors = {"Ghost_chu", "BlackFoxSAR", "BigBrother"},
        dependencies = {
                @Dependency(id = "litebans")
        }
)
@Getter
public class BiliWhiteListVelocity implements SimpleCommand {
    public static final String PREFIX = "BiliWhiteList";
    public static BiliWhiteListVelocity instance;
    private CacheForwardingService resolver;
    private ProfileCache cache;
    private WhiteListManager whiteListManager;
    private BiliDatabase databaseManager;
    private LiteBansListener liteBansListener;
    private JoinListener joinListener;

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final PluginManager pluginManager;

    @Inject
    public BiliWhiteListVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, PluginManager pluginManager) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.pluginManager = pluginManager;
        instance = this;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 加载插件配置
        Config.loadConfig(this);
        // 初始化数据库
        initDatabase();
        // 注册监听器
        registerListener();
        // 注册指令
        registerCommands();
    }

    private void registerListener() {
        joinListener = new JoinListener(this);
        server.getEventManager().register(this, joinListener);
        liteBansListener = new LiteBansListener(this);
        Events.get().register(liteBansListener);
    }

    private void initDatabase() {
        // 初始化NameMapping
        try {
            this.cache = new SQLiteCache(new File(dataDirectory.toFile(), "cache.db"));
        } catch (Throwable throwable) {
            this.cache = new HashMapCache();
        }

        this.resolver = new CacheForwardingService(HttpRepositoryServicePatched.forMinecraft(), cache);
        @SuppressWarnings("unchecked")
        Map<String, Object> mysql = (Map<String, Object>) Config.getConfig().get("mysql");
        this.databaseManager = new BiliDatabase(this,
                (String) mysql.get("host"),
                (String) mysql.get("user"),
                (String) mysql.get("pass"),
                (String) mysql.get("database"),
                (Integer) mysql.get("port"),
                (Boolean) mysql.get("usessl"));
        this.whiteListManager = new WhiteListManager(this);
    }

    /**
     * 注册所有指令
     */
    private void registerCommands() {
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

        //bcreload
        CommandMeta commandMeta6 = commandManager.metaBuilder("bcreload")
                .plugin(this)
                .build();
        commandManager.register(commandMeta6, this);
    }

    /**
     * 重载指令实现
     */
    @Override
    public void execute(Invocation invocation) {
        // 关闭数据源
        this.databaseManager.getDs().close();
        // 取消监听器
        server.getEventManager().unregisterListener(this, joinListener);
        Events.get().unregister(liteBansListener);
        // 加载插件配置
        Config.loadConfig(this);
        // 初始化数据库
        initDatabase();
        if (invocation.source() instanceof Player player) {
            player.sendMessage(Utils.coloredMessage("&a配置文件重载完成"));
        }
        // 注册监听器
        registerListener();
        logger.info("配置文件重载完成");
    }

    /**
     * 重载指令权限检查
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bcreload");
    }
}
