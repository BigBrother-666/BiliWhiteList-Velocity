package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import net.kyori.adventure.text.TextComponent;
import org.enginehub.squirrelid.Profile;

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
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onServerSwitch(ServerPreConnectEvent event) {
        String username = event.getPlayer().getUsername();
        UUID uniqueId = event.getPlayer().getUniqueId();
        String previousServerName = null;
        if (event.getPreviousServer() != null) {
            previousServerName = event.getPreviousServer().getServerInfo().getName();
        }
        String targetServerName = event.getOriginalServer().getServerInfo().getName();
        if (!plugin.getWhiteListManager().isSeverRequireWhiteList(targetServerName)) {
            plugin.getLogger().info("玩家 {} # {} 例外列表放行：{}", username, uniqueId, targetServerName);
            return;
        }
        WhiteListManager.RecordStatus status = plugin.getWhiteListManager().checkWhiteList(uniqueId);
        if (status != WhiteListManager.RecordStatus.WHITELISTED) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            if (previousServerName != null) {
                event.getPlayer().sendMessage(Utils.coloredMessage(((String) messages.get("no-whitelist-switch")).replace("{server}", targetServerName)));
            } else {
                event.getPlayer().disconnect(Utils.coloredMessage((String) messages.get("no-whitelist")));
                plugin.getLogger().info("玩家 {} # {} 没有白名单，已拒绝", username, uniqueId);
            }
        } else {
            plugin.getLogger().info("玩家 {} # {} 白名单放行：{}", username, uniqueId, targetServerName);
        }
    }
}
