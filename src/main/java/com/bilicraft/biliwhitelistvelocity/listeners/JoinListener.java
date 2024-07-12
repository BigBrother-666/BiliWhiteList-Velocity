package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.TextComponent;
import org.enginehub.squirrelid.Profile;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class JoinListener {
    private final BiliWhiteListVelocity plugin;
    private final Map<String, Object> messages;

    public JoinListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
        this.messages = (Map<String, Object>) Config.getConfig().get("messages");
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerJoin(PreLoginEvent event) {
        UUID playerUniqueId = event.getUniqueId();
        String playerName = event.getUsername();

        if (playerUniqueId == null) {
            TextComponent kickMessage = Utils.coloredMessage((String) messages.get("messages.no-licensed-account"));
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
            plugin.getLogger().info("玩家 {} 不是正版 Minecraft 账号，已拒绝", playerName);
            return;
        }
        plugin.getCache().put(new Profile(playerUniqueId, playerName));
        // 获取玩家加入的服务器
        String forcedHost = getForcedHost(event.getConnection().getVirtualHost().get().getHostString());
//        String serverName = null;
//        if (server != null) {
//            serverName = server.getServerInfo().getName();
//        } else {
//            // test
//            plugin.getLogger().error("玩家 {} # {} 连接服务器错误：server=null", playerName, playerUniqueId);
//        }

        if (!plugin.getWhiteListManager().isSeverRequireWhiteList(forcedHost)) {
            plugin.getLogger().info("玩家 {} # {} 例外列表放行：{}", playerName, playerUniqueId, forcedHost);
            return;
        }
        WhiteListManager.RecordStatus status = plugin.getWhiteListManager().checkWhiteList(playerUniqueId);
        if (status != WhiteListManager.RecordStatus.WHITELISTED) {
            TextComponent kickMessage = Utils.coloredMessage((String) messages.get("no-whitelist"));
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(kickMessage));
            plugin.getLogger().info("玩家 {} # {} 没有白名单，已拒绝", playerName, playerUniqueId);
        } else {
            plugin.getLogger().info("玩家 {} # {} 白名单放行", playerName, playerUniqueId);
        }
    }

    private Map<String, String> getForcedHosts() {
        Map<String, String> hosts = new HashMap<>();
        for (Map.Entry<String, String> host : plugin.getServer().getConfiguration().getServers().entrySet()) {
            hosts.put(host.getKey().toLowerCase(), host.getValue().toLowerCase());
        }
        return hosts;
    }

    private String getForcedHost(String virtualHost) { //Maybe null
        return getForcedHosts().get(virtualHost);
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerSwitch(ServerPreConnectEvent event) {
        String username = event.getPlayer().getUsername();
        String targetServerName = event.getOriginalServer().getServerInfo().getName();
        if (!plugin.getWhiteListManager().isSeverRequireWhiteList(targetServerName)) {
            plugin.getLogger().info("玩家 {} # {} 例外列表放行： {}", username, event.getPlayer().getUniqueId(), targetServerName);
            return;
        }
        WhiteListManager.RecordStatus status = plugin.getWhiteListManager().checkWhiteList(event.getPlayer().getUniqueId());
        if (status != WhiteListManager.RecordStatus.WHITELISTED) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Utils.coloredMessage(((String) messages.get("no-whitelist-switch")).replace("{server}", targetServerName)));
        } else {
            plugin.getLogger().info("玩家 {} # {} 白名单放行： {}", username, event.getPlayer().getUniqueId(), targetServerName);
        }
    }
}
