package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerMarkCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public ServerMarkCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Utils.coloredMessage("&c参数错误: /bcservermark <server-name-in-proxy> <bool>"));
            return;
        }
        source.sendMessage(Utils.coloredMessage("正在处理..."));
        if(args[1].equalsIgnoreCase("true")) {
            // Mark require whitelist
            plugin.getWhiteListManager().markServerRequireWhiteList(args[0]);
            source.sendMessage(Utils.coloredMessage("设置成功，服务器 "+ args[0] +" 现在需要白名单了"));
        }else{
            plugin.getWhiteListManager().unmarkServerRequireWhiteList(args[0]);
            source.sendMessage(Utils.coloredMessage("设置成功，服务器 "+ args[0] +" 现在不需要白名单了"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return SimpleCommand.super.suggestAsync(invocation);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return SimpleCommand.super.hasPermission(invocation);
    }
}
