package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ServerMarkCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;
    private final List<String> allServerName;

    public ServerMarkCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
        allServerName = getAllServerName();
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
        if (!allServerName.contains(args[0])){
            source.sendMessage(Utils.coloredMessage("&c服务器名称错误，可用的服务器名称：" + allServerName));
        }
        if (args[1].equalsIgnoreCase("true")) {
            // Mark require whitelist
            plugin.getWhiteListManager().markServerRequireWhiteList(args[0]);
            source.sendMessage(Utils.coloredMessage("设置成功，服务器 "+ args[0] +" 现在需要白名单了"));
        } else if (args[1].equalsIgnoreCase("false")){
            plugin.getWhiteListManager().unmarkServerRequireWhiteList(args[0]);
            source.sendMessage(Utils.coloredMessage("设置成功，服务器 "+ args[0] +" 现在不需要白名单了"));
        } else {
            source.sendMessage(Utils.coloredMessage("&c无效的布尔值"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return getAllServerName();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.admin");
    }

    private List<String> getAllServerName(){
        List<String> allServerName = new ArrayList<>();
        plugin.getServer().getAllServers().forEach(server -> {
            allServerName.add(server.getServerInfo().getName());
        });
        return allServerName;
    }
}
