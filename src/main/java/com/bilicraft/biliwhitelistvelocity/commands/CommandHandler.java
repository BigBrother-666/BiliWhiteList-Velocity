package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.UUID;

public class CommandHandler {

    private final BiliWhiteListVelocity plugin;

    public CommandHandler(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    /**
     * 输出插件基本信息
     */
    public int about(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        String status = Config.isEnabled() ? "&2&lON" : "&c&lOFF";
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "Whitelist is " + status));
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "BiliWhitelist-Velocity by BigBrother"));
        return 1;
    }


    /**
     * 添加白名单
     */
    public int add(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();

//        String sender = "";
//        if (source instanceof Player) {
//            sender = ((Player) source).getUsername();
//        } else if (source instanceof ConsoleCommandSource){
//            sender = "Console";
//        }
        try {
            Profile profile = getPlayerUUID(commandSourceCommandContext);
            if (profile == null){
                return 1;
            }
            UUID uuid = profile.getUniqueId();
            String username = profile.getName();
            switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case BLOCKED:
                            source.sendMessage(coloredMessage("&c添加失败：" + username + " 位于回绝名单中"));
                            return 1;
                        case WHITELISTED:
                            source.sendMessage(coloredMessage("&e添加失败：" + username + " 已在白名单中"));
                            return 1;
                        case NO_RECORD:
                            plugin.getWhiteListManager().addWhite(uuid,new UUID(0,0));
                            source.sendMessage(coloredMessage("&a添加成功：" + username + " # " + uuid));
                            plugin.getLogger().info("&a白名单添加成功：{} # {}, 操作员：{}", username, uuid, commandSourceCommandContext.getInput());
                            return 1;
                    }
        } catch (InterruptedException | IOException e) {
            source.sendMessage(coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + e.getMessage()));
        }
        return 1;
    }

    public int remove(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();

        try {
            Profile profile = getPlayerUUID(commandSourceCommandContext);
            if (profile == null){
                return 1;
            }
            UUID uuid = profile.getUniqueId();
            String username = profile.getName();

            switch (plugin.getWhiteListManager().checkWhiteList(uuid)){
                        case NO_RECORD:
                            source.sendMessage(coloredMessage("&c删除失败：" + username + " 不在白名单或者回绝列表中"));
                            return 1;
                        case WHITELISTED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&a白名单删除成功：{} # {}, 操作员：{}", username, uuid, commandSourceCommandContext.getInput());
                            source.sendMessage(coloredMessage("&a白名单删除：" + username + " # " + uuid));
                            return 1;
                        case BLOCKED:
                            plugin.getWhiteListManager().removeWhite(uuid);
                            plugin.getLogger().info("&e回绝删除成功，如有需要，请重新添加白名单：{} # {}, 操作员：{}", username, uuid, commandSourceCommandContext.getInput());
                            source.sendMessage(coloredMessage("&e回绝删除：" + username + " # " + uuid));
                            return 1;
                    }
        } catch (InterruptedException | IOException e) {
            source.sendMessage(coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + e.getMessage()));
        }
        return 1;
    }



    /**
     * 重新加载配置
     */
    public int reload(CommandContext<CommandSource> commandSourceCommandContext) {
        Config.loadConfig(plugin);
        CommandSource source = commandSourceCommandContext.getSource();
        source.sendMessage(coloredMessage("&aReloaded"));
        return 1;
    }

    private TextComponent coloredMessage(String msg){
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    private Profile getPlayerUUID(CommandContext<CommandSource> commandSourceCommandContext) throws IOException, InterruptedException {
        CommandSource source = commandSourceCommandContext.getSource();
        ParsedArgument<CommandSource, ?> username = commandSourceCommandContext.getArguments().get("username");
        if(username == null) {
            source.sendMessage(coloredMessage("&c参数错误: /bcwhitelist <add/remove/list/query/block> [name/uuid]"));
            return null;
        }
        String usernameStr = (String) username.getResult();

        source.sendMessage(coloredMessage("&b正在处理..."));

        Profile profile = plugin.getResolver().findByName(usernameStr);
        if (profile == null) {
            source.sendMessage(coloredMessage("&c该玩家不存在"));
            return null;
        }
        return profile;
    }
}
