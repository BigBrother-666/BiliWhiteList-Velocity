package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class InviteCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public InviteCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if(!(source instanceof Player)){
            source.sendMessage(Utils.coloredMessage("&c该命令仅可在游戏内执行，后台请使用：bcwhitelist add [name/uuid]"));
            return;
        }
        if (args.length < 1) {
            source.sendMessage(Utils.coloredMessage("&c命令输入有误，正确输入：/bcinvite <游戏ID>"));
            return;
        }
        if (args.length == 1) {
            source.sendMessage(Utils.coloredMessage("&3您正在邀请玩家 &e" + args[0] + " &3加入服务器"));
            source.sendMessage(Utils.coloredMessage("&3邀请成功后，您邀请的玩家将会自动获得白名单"));
            source.sendMessage(Utils.coloredMessage("&e注意：如果您邀请的玩家发生了违规行为，您将会承担连带责任"));
            source.sendMessage(Utils.coloredMessage("&a确认邀请请输入 &6" + "/bcinvite " + args[0] + " confirm"));
            return;
        }

        if (args.length == 2 && args[1].equals("confirm")) {
            source.sendMessage(Utils.coloredMessage("&b正在处理，请稍等..."));
            try {
                Profile profile = plugin.getResolver().findByName(args[0]);
                if(profile == null){
                    source.sendMessage(Utils.coloredMessage("&c您所邀请的玩家不存在，请检查用户名输入是否正确"));
                    return;
                }
                UUID invited = profile.getUniqueId();
                UUID inviter = ((Player) source).getUniqueId();
                String inviterUsername = ((Player) source).getUsername();

                if(plugin.getWhiteListManager().checkWhiteList(inviter) != WhiteListManager.RecordStatus.WHITELISTED){
                    source.sendMessage(Utils.coloredMessage("&c在邀请其他人之前，您需要先通过白名单认证！"));
                    return;
                }

                switch (plugin.getWhiteListManager().checkWhiteList(invited)){
                    case BLOCKED:
                        source.sendMessage(Utils.coloredMessage("&c您所邀请的玩家已被管理组回绝，无法邀请"));
                        return;
                    case WHITELISTED:
                        source.sendMessage(Utils.coloredMessage("&c您所邀请的玩家当前已在白名单中，无需重复邀请"));
                        return;
                    case NO_RECORD:
                        plugin.getWhiteListManager().addWhite(invited,inviter);
                        source.sendMessage(Utils.coloredMessage("&a邀请成功"));
                        plugin.getLogger().info("玩家 {} 邀请了 {}", inviterUsername, args[0]);
                        Utils.broadcast("玩家 " + inviterUsername + " 邀请了 " + args[0]);
                        break;
                }
            } catch (IOException | InterruptedException exception) {
                source.sendMessage(Utils.coloredMessage("&c网络错误，请稍后重试。错误代码：&7" + exception.getMessage()));
            }
        } else {
            source.sendMessage(Utils.coloredMessage("&c命令输入有误，正确输入：/bcinvite <游戏ID>"));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return SimpleCommand.super.suggest(invocation);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.completedFuture(Utils.getAllPlayerName());
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bcinvite");
    }
}
