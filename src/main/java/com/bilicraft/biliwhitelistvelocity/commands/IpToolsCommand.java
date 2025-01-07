package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.IpRecordManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IpToolsCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;

    public IpToolsCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Utils.coloredMessage("&c/bciptool search <玩家ID> : 查询和某玩家使用相同ip登录过的玩家。注：该指令列出的玩家不一定是玩家的小号，需要综合ip属地等判断。\n" +
                    "&c/bciptool iphistory <玩家ID> : 查看某玩家的登录ip及属地统计信息"));
        }

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            switch (args[0]) {
                case "search":
                    source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后..."));
                    Component searchOutput = Component.text("和%s使用相同ip登录过的玩家（指针移动到玩家名上查看详情）：\n".formatted(getHistoryNamesStr(args[1])), NamedTextColor.GREEN);
                    Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(args[1]);
                    if (players.isEmpty()) {
                        source.sendMessage(Component.text("没有查询到和%s使用相同ip登录过的玩家".formatted(getHistoryNamesStr(args[1])), NamedTextColor.RED));
                        return;
                    }

                    for (Map.Entry<String, ArrayList<IpRecordManager.SameIpStats>> entry : players.entrySet()) {
                        String historyNamesStr = getHistoryNamesStr(entry.getKey());
                        Component hoverText = Component.text("%s 和 %s 使用相同ip详情：\n".formatted(historyNamesStr, args[1]), NamedTextColor.GREEN);
                        for (IpRecordManager.SameIpStats sameIpStats : entry.getValue()) {
                            hoverText = hoverText.append(Component.text("%s(%s) | %d次\n".formatted(sameIpStats.getIp(), sameIpStats.getIpLocation(), sameIpStats.getCount()), NamedTextColor.GOLD));
                        }

                        Component temp = Component.text(historyNamesStr + "\n", NamedTextColor.GOLD);
                        temp = temp.hoverEvent(HoverEvent.showText(hoverText));
                        searchOutput = searchOutput.append(temp);
                    }
                    source.sendMessage(searchOutput);
                    break;
                case "iphistory":
                    source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后..."));
                    Component iphistoryOutput = Component.text("%s的登录信息：\n".formatted(getHistoryNamesStr(args[1])), NamedTextColor.GREEN);
                    List<IpRecordManager.IpLocationStats> playerIpLocationRatio = plugin.getIpRecordManager().getPlayerIpLocationRatio(args[1]);

                    if (playerIpLocationRatio.isEmpty()) {
                        source.sendMessage(Component.text("没有查询到%s的登录信息".formatted(getHistoryNamesStr(args[1])), NamedTextColor.RED));
                        return;
                    }

                    int total = IpRecordManager.IpLocationStats.getTotalLogin(playerIpLocationRatio);
                    for (IpRecordManager.IpLocationStats ipLocationStats : playerIpLocationRatio) {
                        double ratio = (double) ipLocationStats.getCount() / total * 100;
                        iphistoryOutput = iphistoryOutput.append(Component.text("%s | %d次 | 占比%.2f%%\n".formatted(ipLocationStats.getIpLocation(), ipLocationStats.getCount(), ratio), NamedTextColor.GOLD));
                    }
                    source.sendMessage(iphistoryOutput);
                    break;
                default:
                    source.sendMessage(Utils.coloredMessage("&c/bciptool search <玩家ID> : 查询和某玩家使用相同ip登录过的玩家。注：该指令列出的玩家不一定是玩家的小号，需要综合ip属地等判断。\n" +
                            "&c/bciptool iphistory <玩家ID> : 查看某玩家的登录ip及属地统计信息"));
            }
        }).schedule();
    }

    private @NotNull String getHistoryNamesStr(String playerName) {
        List<String> playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByName(playerName);
        String historyNamesStr;
        if (playerHistoryNames.size() <= 1) {
            historyNamesStr = playerName;
        } else {
            String newName = playerHistoryNames.getLast();
            playerHistoryNames.removeLast();
            historyNamesStr = newName + "（曾用名：" + String.join(", ", playerHistoryNames) + "）";
        }
        return historyNamesStr;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0)
            return List.of("search", "iphistory");
        else
            return Utils.getAllPlayerName();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bciptool");
    }
}
