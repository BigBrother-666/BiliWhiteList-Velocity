package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.UUID;
import java.util.StringJoiner;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


public class WhiteListCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public WhiteListCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if ((args.length == 1 && !args[0].equals("list")) || args.length == 0 ) {
            source.sendMessage(Utils.coloredMessage("&c参数错误: /bcwhitelist <add/remove/query/block> [name/uuid] 或 /bcwhitelist list"));
            return;
        }
        source.sendMessage(Utils.coloredMessage("&b正在处理..."));
        String sender = "";
        if (source instanceof Player) {
            sender = ((Player) source).getUsername();
        } else if (source instanceof ConsoleCommandSource){
            sender = "Console";
        }

        try {
            UUID uuid = null;
            // 非list指令需要uuid
            if (!args[0].equals("list")){
                Profile profile = plugin.getResolver().findByName(args[1]);
                if (profile == null) {
                    source.sendMessage(Utils.coloredMessage("&c该玩家不存在"));
                    return;
                }
                uuid = profile.getUniqueId();
            }

            switch (args[0]) {
                case "add":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case BLOCKED:
                            source.sendMessage(Utils.coloredMessage("&c添加失败：" + args[1] + " 位于回绝名单中"));
                            return;
                        case WHITELISTED:
                            source.sendMessage(Utils.coloredMessage("&e添加失败：" + args[1] + " 已在白名单中"));
                            return;
                        case NO_RECORD:
                            plugin.getWhiteListManager().addWhite(uuid, new UUID(0,0));
                            source.sendMessage(Utils.coloredMessage("&a添加成功：" + args[1] + " # " + uuid));
                            plugin.getLogger().info("&a白名单添加成功：{} # {}, 操作员：{}", args[1], uuid, sender);
                            return;
                    }
                    break;
                case "remove":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case NO_RECORD:
                            source.sendMessage(Utils.coloredMessage("&c删除失败：" + args[1] + " 不在白名单或者回绝列表中"));
                            return;
                        case WHITELISTED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&a白名单删除成功：{} # {}, 操作员：{}", args[1], uuid, sender);
                            source.sendMessage(Utils.coloredMessage("&a白名单删除：" + args[1] + " # " + uuid));
                            return;
                        case BLOCKED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&e回绝删除成功，如有需要，请重新添加白名单：{} # {}, 操作员：{}", args[1], uuid, sender);
                            source.sendMessage(Utils.coloredMessage("&e回绝删除：" + args[1] + " # " + uuid));
                            return;
                    }
                    break;
                case "list":
                    StringJoiner builder = new StringJoiner(",","","");
                    source.sendMessage(Utils.coloredMessage("&b请稍等，这可能需要一会儿..."));
                    List<UUID> queryResultList = plugin.getWhiteListManager().queryRecords().stream().map(
                            WhiteListManager.QueryResult::getUuid
                    ).collect(Collectors.toList());
                    ImmutableList<Profile> queryList = plugin.getResolver().findAllByUuid(queryResultList);
                    for (Profile pro : queryList) {
                        builder.add(pro.getName());
                    }
                    source.sendMessage(Utils.coloredMessage("&a白名单玩家：" + builder));
                    break;
                case "query":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case BLOCKED:
                            source.sendMessage(Utils.coloredMessage("&c目标玩家处于回绝名单中，无法进入内服，且无法再添加他的白名单"));
                            return;
                        case WHITELISTED:
                            source.sendMessage(Utils.coloredMessage("&a目标玩家处于白名单中，可进入内服"));
                            return;
                        case NO_RECORD:
                            source.sendMessage(Utils.coloredMessage("&e目标玩家不在任何名单中，只能进入外服"));
                            return;
                    }
                    break;
                case "block":
                    switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case BLOCKED:
                            source.sendMessage(Utils.coloredMessage("&c目标玩家已处于回绝名单中"));
                            return;
                        case NO_RECORD:
                        case WHITELISTED:
                            plugin.getWhiteListManager().setBlock(uuid,true);
                            source.sendMessage(Utils.coloredMessage("&a成功设置目标玩家状态为回绝"));
                            return;
                    }
                    break;
                default:
                    source.sendMessage(Utils.coloredMessage("&c参数错误: /bcwhitelist <add/remove/query/block> [name/uuid] 或 /bcwhitelist list"));
            }
        } catch (InterruptedException | IOException e) {
            source.sendMessage(Utils.coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + e.getMessage()));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return List.of("add","remove", "query", "block", "list");
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.admin");
    }
}
