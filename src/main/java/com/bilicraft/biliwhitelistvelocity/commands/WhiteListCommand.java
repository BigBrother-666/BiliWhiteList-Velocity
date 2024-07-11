package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.UUID;


public class WhiteListCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public WhiteListCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(coloredMessage("&c参数错误: /bcwhitelist <add/remove/list/query/block> [name/uuid]"));                return;
        }
        source.sendMessage(coloredMessage("&b正在处理..."));
        String sender = "";
        if (source instanceof Player) {
            sender = ((Player) source).getUsername();
        } else if (source instanceof ConsoleCommandSource){
            sender = "Console";
        }

        try {
            Profile profile = plugin.getResolver().findByName(args[1]);
            if (profile == null) {
                source.sendMessage(coloredMessage("&c该玩家不存在"));
                return;
            }
            UUID uuid = profile.getUniqueId();
            switch (args[0]) {
                case "add":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case BLOCKED:
                            source.sendMessage(coloredMessage("&c添加失败：" + args[1] + " 位于回绝名单中"));
                            return;
                        case WHITELISTED:
                            source.sendMessage(coloredMessage("&e添加失败：" + args[1] + " 已在白名单中"));
                            return;
                        case NO_RECORD:
                            plugin.getWhiteListManager().addWhite(uuid,new UUID(0,0));
                            source.sendMessage(coloredMessage("&a添加成功：" + args[1] + " # " + uuid));
                            plugin.getLogger().info("&a白名单添加成功：{} # {}, 操作员：{}", args[1], uuid, invocation);
                            return;
                    }
                    break;
                case "remove":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case NO_RECORD:
                            source.sendMessage(coloredMessage("&c删除失败：" + args[1] + " 不在白名单或者回绝列表中"));
                            return;
                        case WHITELISTED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&a白名单删除成功：{} # {}, 操作员：{}", args[1], uuid, sender);
                            source.sendMessage(coloredMessage("&a白名单删除：" + args[1] + " # " + uuid));
                            return;
                        case BLOCKED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&e回绝删除成功，如有需要，请重新添加白名单：{} # {}, 操作员：{}", args[1], uuid, sender);
                            source.sendMessage(coloredMessage("&e回绝删除：" + args[1] + " # " + uuid));
                            return;
                    }
                    break;
                default:
                    source.sendMessage(coloredMessage("&a参数有误"));
            }
        } catch (InterruptedException | IOException e) {
            source.sendMessage(coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + e.getMessage()));
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return SimpleCommand.super.hasPermission(invocation);
    }

    private TextComponent coloredMessage(String msg){
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }
}
