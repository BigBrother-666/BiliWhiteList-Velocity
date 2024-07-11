package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class CommandHandler {

    private BiliWhiteListVelocity biliWhitelist;

    public CommandHandler(BiliWhiteListVelocity biliWhitelist) {
        this.biliWhitelist = biliWhitelist;
    }

    /**
     * A bit of basic about information
     * @param commandSourceCommandContext
     * @return
     */
    public int about(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        String status = Config.isEnabled() ? "&2&lON" : "&c&lOFF";
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "Whitelist is " + status));
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "VelocityWhitelist by james090500"));
        return 1;
    }

    /**
     * Turn on the whitelist
     * @param commandSourceCommandContext
     * @return
     */
    public int turnOn(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        if(Config.isEnabled()) {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c" + BiliWhiteListVelocity.PREFIX + "Whitelist is already turned on"));
        } else {
            Config.setEnabled(true);
            Config.saveConfig(biliWhitelist);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "Whitelist turned &2&lON"));
        }
        return 1;
    }

    /**
     * 关闭白名单
     */
    public int turnOff(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        if(!Config.isEnabled()) {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c" + BiliWhiteListVelocity.PREFIX + "Whitelist is already turned off"));
        } else {
            Config.setEnabled(false);
            Config.saveConfig(biliWhitelist);
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "Whitelist turned &c&lOFF"));
        }
        return 1;
    }

    /**
     * The command for /vwhitelist add <username>
     * Handles adding a user to the whitelist
     */
    public int add(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        ParsedArgument<CommandSource, ?> username = commandSourceCommandContext.getArguments().get("username");
        if(username == null) {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c" + BiliWhiteListVelocity.PREFIX + "Syntax /vwhitelist add <username>"));
            return 1;
        }

//        new WhitelistHelper(biliWhitelist, source).add((String) username.getResult());
        return 1;
    }

    /**
     * The command for /vwhitelist remove <username>
     * Handles removing a user from the whitelist
     * @param commandSourceCommandContext
     * @return
     */
    public int remove(CommandContext<CommandSource> commandSourceCommandContext) {
        CommandSource source = commandSourceCommandContext.getSource();
        ParsedArgument<CommandSource, ?> username = commandSourceCommandContext.getArguments().get("username");
        if(username == null) {
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c" + BiliWhiteListVelocity.PREFIX + "Syntax /vwhitelist remove <username>"));
            return 1;
        }

//        new WhitelistHelper(biliWhitelist, source).remove((String) username.getResult());
        return 1;
    }

    /**
     * Reloads the Config
     * @param commandSourceCommandContext
     * @return
     */
    public int reload(CommandContext<CommandSource> commandSourceCommandContext) {
        Config.loadConfig(biliWhitelist);
        CommandSource source = commandSourceCommandContext.getSource();
        source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&a" + BiliWhiteListVelocity.PREFIX + "Reloaded"));
        return 1;
    }
}