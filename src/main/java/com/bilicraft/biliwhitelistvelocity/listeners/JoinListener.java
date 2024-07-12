package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.TextComponent;
import org.enginehub.squirrelid.Profile;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;

import java.util.Map;
import java.util.UUID;

public class JoinListener {
    private final BiliWhiteListVelocity plugin;

    public JoinListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerJoin(LoginEvent event) {
        UUID playerUniqueId = event.getPlayer().getUniqueId();
        String playerName = event.getPlayer().getUsername();
        @SuppressWarnings("unchecked")
        Map<String, Object> messages = (Map<String, Object>) Config.getConfig().get("messages");

        if (playerUniqueId == null) {
            TextComponent kickMessage = Utils.coloredMessage((String) messages.get("messages.no-licensed-account"));
            event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
            plugin.getLogger().info("玩家 {} 不是正版 Minecraft 账号，已拒绝", playerName);
            return;
        }
        plugin.getCache().put(new Profile(playerUniqueId, playerName));
        // 获取玩家加入的服务器
        RegisteredServer server = event.getPlayer().getCurrentServer().map(ServerConnection::getServer).orElse(null);
        String serverName;
        if (server != null) {
            serverName = server.getServerInfo().getName();
        } else {
            plugin.getLogger().error("玩家 {} # {} 连接服务器错误：server=null", playerName, playerUniqueId);
            return;
        }

        if (!plugin.getWhiteListManager().isSeverRequireWhiteList(serverName)) {
            plugin.getLogger().info("玩家 {} # {} 例外列表放行：{}", playerName, playerUniqueId, serverName);
            return;
        }
        WhiteListManager.RecordStatus status = plugin.getWhiteListManager().checkWhiteList(playerUniqueId);
        if (status != WhiteListManager.RecordStatus.WHITELISTED) {
            TextComponent kickMessage = Utils.coloredMessage((String) messages.get("messages.no-whitelist"));
            event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
            plugin.getLogger().info("玩家 {} # {} 没有白名单，已拒绝", playerName, playerUniqueId);
        } else {
            plugin.getLogger().info("玩家 {} # {} 白名单放行", playerName, playerUniqueId);
        }
    }
}
