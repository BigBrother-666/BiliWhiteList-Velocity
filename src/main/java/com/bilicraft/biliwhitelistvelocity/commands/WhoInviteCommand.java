package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class WhoInviteCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public WhoInviteCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }


    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            source.sendMessage(Utils.coloredMessage("&c用法: /whoinvite <玩家ID>"));
            return;
        }
        source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后..."));

        try {
            Profile profile = plugin.getResolver().findByName(args[0]);
            if (profile == null) {
                source.sendMessage(Utils.coloredMessage("&c您所邀请的玩家不存在，请检查用户名输入是否正确"));
                return;
            }
            WhiteListManager.QueryResult result = plugin.getWhiteListManager().queryRecord(profile.getUniqueId());
            if (result == null) {
                source.sendMessage(Utils.coloredMessage("&c该玩家无人邀请或网络故障"));
                return;
            }
            if(result.getInviter().equals(new UUID(0,0))){
                source.sendMessage(Utils.coloredMessage("&a查询结果: 管理员操作"));
                return;
            }
            Profile inviter = plugin.getResolver().findByUuid(result.getInviter());
            if (inviter == null) {
                source.sendMessage(Utils.coloredMessage("&c该玩家无人邀请、为虚拟玩家或者网络故障"));
                return;
            }
            source.sendMessage(Utils.coloredMessage("&a查询结果: &e" + inviter.getName()));
        } catch (IOException | InterruptedException exception) {
            source.sendMessage(Utils.coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + exception.getMessage()));
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
        return invocation.source().hasPermission("biliwhitelist.admin");
    }
}
