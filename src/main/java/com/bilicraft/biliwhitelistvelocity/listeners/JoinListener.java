package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;

import java.net.InetSocketAddress;
import java.util.UUID;

public class JoinListener {
    private final BiliWhiteListVelocity plugin;

    public JoinListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPlayerJoin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID playerUniqueId = player.getUniqueId();
        String playerName = player.getUsername();
        InetSocketAddress ip = player.getRemoteAddress();

        if (!player.isOnlineMode()) {
            TextComponent kickMessage = Utils.coloredMessage((String) Config.getMessagesConf().getOrDefault("no-licensed-account", "请使用正版 Minecraft 账号登录"));
            event.setResult(ResultedEvent.ComponentResult.denied(kickMessage));
            plugin.getLogger().info("玩家 {} 不是正版 Minecraft 账号，已拒绝", playerName);
        } else if (ip != null && ip.getAddress() != null) {
            plugin.getIpRecordManager().addRecord(playerName, playerUniqueId, ip.getAddress().getHostAddress());
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerSwitch(ServerPreConnectEvent event) {
        String username = event.getPlayer().getUsername();
        UUID uniqueId = event.getPlayer().getUniqueId();
        String previousServerName = null;
        String targetServerName = event.getOriginalServer().getServerInfo().getName();
        if (checkBypass(event.getPlayer())){
            plugin.getLogger().info("玩家 {} # {} 具有bypass权限，放行：{}", username, uniqueId, targetServerName);
            return;
        }
        if (event.getPreviousServer() != null) {
            previousServerName = event.getPreviousServer().getServerInfo().getName();
        }
        if (!plugin.getWhiteListManager().isSeverRequireWhiteList(targetServerName)) {
            plugin.getLogger().info("玩家 {} # {} 例外列表放行：{}", username, uniqueId, targetServerName);
            return;
        }
        WhiteListManager.RecordStatus status = plugin.getWhiteListManager().checkWhiteList(uniqueId);
        if (status != WhiteListManager.RecordStatus.WHITELISTED) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            if (previousServerName != null) {
                event.getPlayer().sendMessage(Utils.coloredMessage(((String) Config.getMessagesConf().getOrDefault("no-whitelist-switch", "抱歉，您没有白名单，无法连接到 {server} 服务器，请先申请")).replace("{server}", targetServerName)));
            } else {
                event.getPlayer().disconnect(Utils.coloredMessage((String) Config.getMessagesConf().getOrDefault("no-whitelist", "抱歉，您没有此服务器的白名单，请先申请")));
                plugin.getLogger().info("玩家 {} # {} 没有白名单，已拒绝", username, uniqueId);
            }
        } else {
            plugin.getLogger().info("玩家 {} # {} 白名单放行：{}", username, uniqueId, targetServerName);
        }
    }

    private boolean checkBypass(Player player) {
        return player.hasPermission("biliwhitelist.bypass");
    }
}
