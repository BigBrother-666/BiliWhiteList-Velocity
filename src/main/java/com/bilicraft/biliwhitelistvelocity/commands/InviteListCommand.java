package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.WhiteListManager;
import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import org.enginehub.squirrelid.Profile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class InviteListCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public InviteListCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length != 1) {
            source.sendMessage(Utils.coloredMessage("&c用法: /bcinvitelist <玩家ID>"));
            return;
        }
        source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后（这可能需要很长一段时间）..."));

        try {
            Profile profile = plugin.getResolver().findByName(args[0]);
            if (profile == null) {
                source.sendMessage(Utils.coloredMessage("&c您所查询的玩家不存在，请检查用户名输入是否正确"));
                return;
            }

            List<UUID> inviteds = plugin.getWhiteListManager().queryRecords().stream().map(WhiteListManager.QueryResult::getInviter).toList();

            for (UUID invited : inviteds) {
                try {
                    // 跳过非邀请加入的玩家
                    if (invited.equals(new UUID(0, 0))){
                        continue;
                    }
                    Profile invitedProfile = plugin.getResolver().findByUuid(invited);
                    if (invitedProfile != null) {
                        source.sendMessage(Utils.coloredMessage("&e- &3" + invitedProfile.getName() + "&8 (" + invitedProfile.getUniqueId() + ")"));
                    }
                } catch (IllegalArgumentException exception){
                    source.sendMessage(Utils.coloredMessage("&e- &3" + "读取失败" + "&8 (" + invited.toString() + ")"));
                }
            }
            source.sendMessage(Utils.coloredMessage("&b查询完毕！"));
        } catch (IOException | InterruptedException e) {
            source.sendMessage(Utils.coloredMessage("&c内部错误，请稍后重试。错误代码：&7" + e.getMessage()));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0)
            return Utils.getAllPlayerName();
        else
            return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bcinvitelist");
    }
}
