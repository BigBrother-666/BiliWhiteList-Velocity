package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import litebans.api.Entry;
import litebans.api.Events;
import org.enginehub.squirrelid.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class LiteBansListener extends Events.Listener {
    private final BiliWhiteListVelocity plugin;
    @SuppressWarnings("unchecked")
    private final Map<String, Object> conf = (Map<String, Object>) Config.getConfig().get("joint-liability");

    public LiteBansListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void entryAdded(Entry entry) {
        switch (entry.getType()) {
            case "ban":
                // This is a ban event.
                if (!(Boolean) conf.get("enable")) {
                    break;
                }
                long configDuration = (int) conf.get("invitee-ban-duration");
                if (configDuration < 0 && entry.getDuration() == -1 || configDuration >= 0 && entry.getDuration() >= configDuration) {
                    String[] ret = getInviter(entry.getUuid(), entry.getExecutorUUID());
                    if (ret == null) {
                        break;
                    }
                    String inviterUuid = ret[0];
                    String inviterName = ret[1];
                    String inviteeName = ret[2];
                    // 符合连带处罚条件
                    punish(inviterUuid, inviterName, inviteeName, entry.getExecutorUUID());
                }
                break;
            case "mute":
                // This is a mute event.
                break;
            case "warn":
                // This is a warn event.
                break;
            case "kick":
                // This is a kick event.
                break;
        }
    }

    @Override
    public void broadcastSent(@NotNull String message, @Nullable String type) {

    }

    private void punish(String inviterUuid, String inviterName, String inviteeName, String executorUUID) {
        CommandSource commandSource = getCommandSource(executorUUID);
        if (commandSource == null) {
            return;
        }
        // 检查inviter是否在排除列表中
        @SuppressWarnings("unchecked")
        List<String> whitelist = (List<String>) conf.getOrDefault("inviter-whitelist", Collections.emptyList());
        if (whitelist != null && whitelist.contains(inviterUuid)) {
            // 不处罚
            commandSource.sendMessage(Utils.coloredMessage("&a" + inviterName + "在连带处罚白名单中，不进行处罚"));
            return;
        }

        // 执行处罚指令
        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) conf.getOrDefault("inviter-punishment", Collections.emptyList());
        if (cmd != null) {
            ProxyServer server = plugin.getServer();
            for (String s : cmd) {
                s = s.replace("{inviter}", inviterName).replace("{invitee}", inviteeName);
                server.getCommandManager().executeAsync(commandSource, s);
            }
        }
        commandSource.sendMessage(Utils.coloredMessage("&a连带处罚执行完成"));
    }

    private String[] getInviter(String inviteeUuid, String executorUUID) {
        CommandSource source;
        source = getCommandSource(executorUUID);
        if (source == null) {
            return null;
        }

        try {
            Profile profile = plugin.getResolver().findByUuid(UUID.fromString(inviteeUuid));
            if (profile == null) {
                source.sendMessage(Utils.coloredMessage("&c查询上级邀请人时发生错误：所查询的玩家不存在"));
                return null;
            }
            source.sendMessage(Utils.coloredMessage("&a触发连带处罚机制，查询" + profile.getName() + "的上级邀请人..."));
            WhiteListManager.QueryResult result = plugin.getWhiteListManager().queryRecord(profile.getUniqueId());
            if (result == null) {
                source.sendMessage(Utils.coloredMessage("&c" + profile.getName() + "无人邀请或网络故障"));
                return null;
            }
            if (result.getInviter().equals(new UUID(0, 0))) {
                source.sendMessage(Utils.coloredMessage("&a" + profile.getName() + "邀请人查询结果: 管理员操作"));
                return null;
            }
            Profile inviter = plugin.getResolver().findByUuid(result.getInviter());
            if (inviter == null) {
                source.sendMessage(Utils.coloredMessage("&c" + profile.getName() + "无人邀请、为虚拟玩家或者网络故障"));
                return null;
            }
            source.sendMessage(Utils.coloredMessage("&a" + profile.getName() + "的上级邀请人为: &e" + inviter.getName()));
            return new String[]{inviter.getUniqueId().toString().replace("-", ""), inviter.getName(), profile.getName()};
        } catch (IOException | InterruptedException exception) {
            source.sendMessage(Utils.coloredMessage("&c查询上级邀请人时发生内部错误。错误代码：&7" + exception.getMessage()));
            return null;
        }
    }

    private @Nullable CommandSource getCommandSource(String executorUUID) {
        CommandSource source;
        if (executorUUID != null && !executorUUID.equals("[Console]")) {
            // 玩家执行的命令
            Optional<Player> player = plugin.getServer().getPlayer(UUID.fromString(executorUUID));
            if (player.isPresent()) {
                source = player.get();
            } else {
                return null;
            }
        } else {
            // 控制台执行的命令
            source = plugin.getServer().getConsoleCommandSource();
        }
        return source;
    }
}

