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
import org.jetbrains.annotations.Nullable;

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

        if (args.length < 1) {
            source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>] : 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n" +
                    "&c/bciptool iphistory <玩家ID> : 查看某玩家的登录ip及属地统计信息"));
        }

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            switch (args[0]) {
                case "dupeip":
                    source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后..."));
                    String arg = findArg("--range", args);
                    int range = -1;
                    if (arg != null) {
                        try {
                            range = Integer.parseInt(arg);
                            if (range <= 0) {
                                source.sendMessage(Utils.coloredMessage("&crange必须为正整数！"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                            source.sendMessage(Utils.coloredMessage("&crange必须为正整数！"));
                            return;
                        }
                    }
                    if (args.length == 1 || args.length == 3) {
                        // 查询所有封禁玩家
                        for (String uuid : plugin.getIpRecordManager().getBannedPlayersUuid()) {
                            Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(uuid, range);
                            subcommandDupeip(args, source, players);
                        }
                    } else if (!args[1].trim().isEmpty()) {
                        // 查询单个玩家
                        Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(args[1], range);
                        subcommandDupeip(args, source, players);
                    } else {
                        source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>] : 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n"));
                    }
                    break;
                case "iphistory":
                    if (args.length == 1 || args[1].trim().isEmpty()) {
                        source.sendMessage(Utils.coloredMessage("&c/bciptool iphistory <玩家ID> : 查看某玩家的登录ip及属地统计信息"));
                        return;
                    }
                    source.sendMessage(Utils.coloredMessage("&b正在查询，请稍后..."));
                    Component iphistoryOutput = Component.text("%s的登录信息（指针移动到各行可查看详情）：\n".formatted(getHistoryNamesStr(args[1])), NamedTextColor.GREEN);
                    List<IpRecordManager.IpLocationStats> playerIpLocationRatio = plugin.getIpRecordManager().getPlayerIpLocationRatio(args[1]);

                    if (playerIpLocationRatio.isEmpty()) {
                        source.sendMessage(Component.text("没有查询到%s的登录信息".formatted(getHistoryNamesStr(args[1])), NamedTextColor.RED));
                        return;
                    }

                    int total = IpRecordManager.IpLocationStats.getTotalLogin(playerIpLocationRatio);
                    for (IpRecordManager.IpLocationStats ipLocationStats : playerIpLocationRatio) {
                        // 生成悬浮文字
                        Component hoverText = Utils.coloredMessage("&a玩家 %s 登录属地 %s 的ip详情：\n&f-------------------------------------------\n".formatted(args[1], ipLocationStats.getIpLocation()));
                        hoverText = hoverText.append(Utils.coloredMessage("&l     &6ip     &f| &6使用次数 &f|     &6第一次登录时间   &f|      &6最后登录时间  \n"));
                        List<IpRecordManager.IpLocationInfo> info = ipLocationStats.getInfo();
                        for (IpRecordManager.IpLocationInfo ipLocationInfo : info) {
                            hoverText = hoverText.append(Utils.coloredMessage("&6%s &f| &6%d &f| &6%s &f| &6%s\n".formatted(ipLocationInfo.getIp(), ipLocationInfo.getCount(), ipLocationInfo.getFirstLoginTIme(), ipLocationInfo.getLastLoginTIme())));
                        }

                        double ratio = (double) ipLocationStats.getCount() / total * 100;
                        iphistoryOutput = iphistoryOutput.append(Utils.coloredMessage("&6%s &f| &6%d次 &f| &6占比%.2f%%\n".formatted(ipLocationStats.getIpLocation(), ipLocationStats.getCount(), ratio)).hoverEvent(HoverEvent.showText(hoverText)));
                    }
                    source.sendMessage(iphistoryOutput);
                    break;
                default:
                    source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>]: 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n" +
                            "&c/bciptool iphistory <玩家ID> : 查看某玩家的登录ip及属地统计信息"));
            }
        }).schedule();
    }

    @Nullable
    private String findArg(String name, String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name) && args.length > i + 1 && !args[i + 1].trim().isEmpty()) {
                return args[i + 1];
            }
        }
        return null;
    }

    private void subcommandDupeip(String[] args, CommandSource source, Map<String, ArrayList<IpRecordManager.SameIpStats>> players) {
        Component dupeipOutput = Component.text("和%s使用相同ip登录过的玩家（指针移动到玩家名上查看相同ip详情）：\n".formatted(getHistoryNamesStr(args[1])), NamedTextColor.GREEN);
        if (players.isEmpty()) {
            source.sendMessage(Component.text("没有查询到和%s使用相同ip登录过的玩家".formatted(getHistoryNamesStr(args[1])), NamedTextColor.RED));
            return;
        }

        for (Map.Entry<String, ArrayList<IpRecordManager.SameIpStats>> entry : players.entrySet()) {
            String historyNamesStr = getHistoryNamesStr(entry.getKey());
            Component hoverText = Utils.coloredMessage("&a%s 和 %s 使用相同ip详情：\n&f-------------------------------------------\n".formatted(historyNamesStr, args[1]));
            int totalCount = 0;
            for (IpRecordManager.SameIpStats sameIpStats : entry.getValue()) {
                hoverText = hoverText.append(Utils.coloredMessage("&6%s (%s) &f| &6%d次\n".formatted(sameIpStats.getIp(), sameIpStats.getIpLocation(), sameIpStats.getCount())));
                totalCount += sameIpStats.getCount();
            }

            Component temp = Utils.coloredMessage("&6%s &f| &6使用相同ip登录次数：%d\n".formatted(historyNamesStr, totalCount));
            temp = temp.hoverEvent(HoverEvent.showText(hoverText));
            dupeipOutput = dupeipOutput.append(temp);
        }
        source.sendMessage(dupeipOutput);
    }

    private @NotNull String getHistoryNamesStr(String playerNameOrUuid) {
        // uuid
        List<String> playerHistoryNames;
        if (playerNameOrUuid.length() == 36 && playerNameOrUuid.contains("-")) {
            playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByUuid(playerNameOrUuid);
        } else {
            playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByName(playerNameOrUuid);
        }

        String historyNamesStr;
        if (playerHistoryNames.isEmpty()) {
            return playerNameOrUuid;
        } else if (playerHistoryNames.size() == 1) {
            historyNamesStr = playerHistoryNames.getFirst();
        } else {
            String newName = playerHistoryNames.getLast();
            playerHistoryNames.removeLast();
            historyNamesStr = newName + "（曾用名：" + String.join(", ", playerHistoryNames) + "）";
        }
        return historyNamesStr;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return List.of("dupeip", "iphistory");
        } else if (invocation.arguments().length == 1) {
            List<String> suggest = Utils.getAllPlayerName();
            suggest.addFirst("--range");
            return suggest;
        } else if (invocation.arguments().length == 2) {
            return List.of("--range");
        } else {
            return List.of();
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bciptool");
    }
}
