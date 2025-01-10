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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.enginehub.squirrelid.Profile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class LiteBansListener extends Events.Listener {
    private final BiliWhiteListVelocity plugin;

    public LiteBansListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void entryAdded(Entry entry) {
        switch (entry.getType()) {
            case "ban":
                // This is a ban event.
                if (!(Boolean) Config.getJointLiabilityConf().getOrDefault("enable", false)) {
                    break;
                }
                long configDuration = (int) Config.getJointLiabilityConf().getOrDefault("invitee-ban-duration", -1);
                if (configDuration < 0 && entry.isPermanent() || configDuration >= 0 && entry.getDuration() >= configDuration) {
                    String[] ret = getInviter(entry.getUuid(), entry.getExecutorUUID());
                    if (ret == null) {
                        break;
                    } else {
                        CommandSource commandSource = getCommandSource(entry.getExecutorUUID());
                        if (commandSource == null) {
                            return;
                        } else if (commandSource instanceof Player executor) {
                            // 获取邀请人和被邀请人信息
                            String inviterUuid = ret[0];
                            String inviterName = ret[1];
                            String inviteeName = ret[2];

                            if ((Boolean) Config.getJointLiabilityConf().getOrDefault("auto", false)) {
                                punish(inviterUuid, inviterName, inviteeName, executor);
                                return;
                            }

                            // 发送手动确认信息
                            @SuppressWarnings("unchecked")
                            List<String> cmd = (List<String>) Config.getJointLiabilityConf().getOrDefault("inviter-punishment", Collections.emptyList());
                            Component hoverText = Component.text("将要执行的处罚指令：\n");
                            for (String s : cmd) {
                                s = s.replace("{inviter}", inviterName).replace("{invitee}", inviteeName).trim();
                                hoverText = hoverText.append(Component.text(s + "\n", NamedTextColor.RED));
                            }
                            Component confirmText = Component.text("[ 确认对上级邀请人执行连带处罚 ]", NamedTextColor.RED);
                            confirmText = confirmText.hoverEvent(HoverEvent.showText(hoverText));
                            confirmText = confirmText.clickEvent(ClickEvent.callback(audience -> punish(inviterUuid, inviterName, inviteeName, executor)));
                            executor.sendMessage(confirmText);
                        } else {
                            commandSource.sendMessage(Component.text("控制台不支持自动执行连带处罚，请手动执行", NamedTextColor.YELLOW));
                        }
                    }
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

    private void punish(String inviterUuid, String inviterName, String inviteeName, Player executor) {
        // 检查inviter是否在排除列表中
        @SuppressWarnings("unchecked")
        List<String> whitelist = (List<String>) Config.getJointLiabilityConf().getOrDefault("inviter-whitelist", Collections.emptyList());

        if (whitelist != null && whitelist.contains(inviterUuid)) {
            // 不处罚
            executor.sendMessage(Utils.coloredMessage("&e " + inviterName + " &a在连带处罚白名单中，不进行处罚"));
            return;
        }

        // 执行处罚指令
        @SuppressWarnings("unchecked")
        List<String> cmd = (List<String>) Config.getJointLiabilityConf().getOrDefault("inviter-punishment", Collections.emptyList());
        int count = 0;
        ProxyServer server = plugin.getServer();

        for (String s : cmd) {
            s = s.replace("{inviter}", inviterName).replace("{invitee}", inviteeName).trim();
            if (executor.isActive() && !s.startsWith("litebans")) {
                // 非litebans指令由玩家身份执行（如果ban指令由玩家执行）
                executor.spoofChatInput("/" + s);
                count++;
                continue;
            }
            if (executor.isActive() && s.startsWith("litebans")) {
                server.getCommandManager().executeAsync(executor, s);
                count++;
            }
        }

        executor.sendMessage(Utils.coloredMessage("&a连带处罚执行完成，执行了 %d 条指令".formatted(count)));
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
            source.sendMessage(Utils.coloredMessage("&a触发连带处罚机制，查询 " + profile.getName() + " 的上级邀请人..."));
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
            source.sendMessage(Utils.coloredMessage("&a " + profile.getName() + " 的上级邀请人为: &e%s &8(%s)".formatted(inviter.getName(), inviter.getUniqueId().toString())));
            return new String[]{inviter.getUniqueId().toString(), inviter.getName(), profile.getName()};
        } catch (IOException | InterruptedException exception) {
            source.sendMessage(Utils.coloredMessage("&c查询上级邀请人时发生内部错误。错误代码：&7" + exception.getMessage()));
            return null;
        }
    }

    private @Nullable CommandSource getCommandSource(String executorUUID) {
        CommandSource source;
        if (executorUUID != null && !executorUUID.toLowerCase().contains("[console]")) {
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

