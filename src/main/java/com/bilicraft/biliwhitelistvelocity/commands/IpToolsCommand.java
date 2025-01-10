package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.IpRecordManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IpToolsCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;
    private List<String> bannedPlayersUuid;

    public IpToolsCommand(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
        this.bannedPlayersUuid = new ArrayList<>();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>] : 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n" +
                    "&c/bciptool iphistory <玩家ID> [--range <days>] : 查看某玩家的登录ip及属地统计信息"));
            return;
        }

        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            bannedPlayersUuid = plugin.getIpRecordManager().getBannedPlayersUuid();
            // 查找range参数
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

            switch (args[0]) {
                case "dupeip":
                    if (range <= 0) {
                        source.sendMessage(Utils.coloredMessage("&b正在根据全部登录记录查询，请稍后..."));
                    } else {
                        source.sendMessage(Utils.coloredMessage("&b正在根据 %d 天内的登录记录查询，请稍后...".formatted(range)));
                    }
                    source.sendMessage(Utils.coloredMessage("&f========================================================="));
                    if (args.length == 1 || range != -1 && args.length == 3) {
                        // 查询所有封禁玩家
                        for (String uuid : bannedPlayersUuid) {
                            Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(uuid, range);
                            subcommandDupeip(uuid, source, players);
                        }
                    } else if (!args[1].trim().isEmpty() && !args[1].trim().equals("--range")) {
                        // 查询单个玩家
                        Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(args[1], range);
                        subcommandDupeip(args[1], source, players);
                    } else {
                        source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>] : 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n"));
                        return;
                    }
                    break;
                case "iphistory":
                    if (args.length == 1 || args[1].trim().isEmpty()) {
                        source.sendMessage(Utils.coloredMessage("&c/bciptool iphistory <玩家ID> [--range <days>] : 查看某玩家的登录ip及属地统计信息"));
                        return;
                    }
                    if (range <= 0) {
                        source.sendMessage(Utils.coloredMessage("&b正在根据全部登录记录查询，请稍后..."));
                    } else {
                        source.sendMessage(Utils.coloredMessage("&b正在根据 %d 天内的登录记录查询，请稍后...".formatted(range)));
                    }
                    source.sendMessage(Utils.coloredMessage("&f========================================================="));
                    Component iphistoryOutput = Utils.coloredMessage("&6%s &a的登录信息（指针移动到各行可查看详情）：\n".formatted(getHistoryNamesStr(args[1])));
                    List<IpRecordManager.IpLocationStats> playerIpLocationRatio = plugin.getIpRecordManager().getPlayerIpLocationRatio(args[1], range);

                    if (playerIpLocationRatio.isEmpty()) {
                        source.sendMessage(Utils.coloredMessage("&e没有查询到 %s &e的登录信息".formatted(getHistoryNamesStr(args[1]))));
                        return;
                    }

                    int total = IpRecordManager.IpLocationStats.getTotalLogin(playerIpLocationRatio);
                    for (IpRecordManager.IpLocationStats ipLocationStats : playerIpLocationRatio) {
                        // 生成悬浮文字
                        Component hoverText = Utils.coloredMessage("&a玩家 %s &a登录属地 %s 的ip详情：\n&f-------------------------------------------\n".formatted(getHistoryNamesStr(args[1]), ipLocationStats.getIpLocation()));
                        hoverText = hoverText.append(Utils.coloredMessage("&l     &6ip     &f| &6使用次数 &f|     &6第一次登录时间   &f|      &6最后登录时间  \n"));
                        List<IpRecordManager.IpLocationInfo> info = ipLocationStats.getInfo();
                        for (IpRecordManager.IpLocationInfo ipLocationInfo : info) {
                            hoverText = hoverText.append(Utils.coloredMessage("&6%s &f| &6%d &f| &6%s &f| &6%s\n".formatted(ipLocationInfo.getIp(), ipLocationInfo.getCount(), ipLocationInfo.getFirstLoginTIme(), ipLocationInfo.getLastLoginTIme())));
                        }

                        double ratio = (double) ipLocationStats.getCount() / total * 100;
                        iphistoryOutput = iphistoryOutput.append(Utils.coloredMessage("&6%s &f| &6%d次 &f| &6占比%.2f%%\n".formatted(ipLocationStats.getIpLocation(), ipLocationStats.getCount(), ratio)).hoverEvent(HoverEvent.showText(hoverText)));
                    }
                    source.sendMessage(iphistoryOutput);
                    source.sendMessage(Utils.coloredMessage("&f========================================================="));
                    break;
                default:
                    source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>]: 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n" +
                            "&c/bciptool iphistory <玩家ID> [--range <days>] : 查看某玩家的登录ip及属地统计信息"));
                    return;
            }
            source.sendMessage(Utils.coloredMessage("&b查询完成！正在封禁的玩家已用&c红色字体&b标出。"));
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

    private void subcommandDupeip(String playerNameOrUuid, CommandSource source, Map<String, ArrayList<IpRecordManager.SameIpStats>> players) {
        String playerName;
        if (playerNameOrUuid.length() == 36 && playerNameOrUuid.contains("-")) {
            List<String> playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByUuid(playerNameOrUuid);
            if (playerHistoryNames == null || playerHistoryNames.isEmpty()) {
                source.sendMessage(Utils.coloredMessage("&e没有查询到uuid: %s &e的登录记录\n".formatted(getHistoryNamesStr(playerNameOrUuid))));
                source.sendMessage(Utils.coloredMessage("&f========================================================="));
                return;
            }
            playerName = playerHistoryNames.getLast();
            if (playerName == null) {
                source.sendMessage(Utils.coloredMessage("&e没有查询到uuid: %s &e的登录记录\n".formatted(getHistoryNamesStr(playerNameOrUuid))));
                source.sendMessage(Utils.coloredMessage("&f========================================================="));
                return;
            }
        } else {
            playerName = playerNameOrUuid;
        }
        Component dupeipOutput = Utils.coloredMessage("&a和 %s &a使用相同ip登录过的玩家（指针移动到玩家名上查看相同ip详情）：\n".formatted(getHistoryNamesStr(playerName)));
        if (players.isEmpty()) {
            source.sendMessage(Utils.coloredMessage("&e没有查询到和 %s &e使用相同ip登录过的玩家\n".formatted(getHistoryNamesStr(playerName))));
            source.sendMessage(Utils.coloredMessage("&f========================================================="));
            return;
        }

        TreeMap<Integer, Component> treeMap = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<String, ArrayList<IpRecordManager.SameIpStats>> entry : players.entrySet()) {
            String historyNamesStr = getHistoryNamesStr(entry.getKey());
            Component hoverText = Utils.coloredMessage("&6%s &a和 %s &a使用相同ip详情：\n&f-------------------------------------------\n".formatted(historyNamesStr, getHistoryNamesStr(playerName)));
            int totalCount = 0;
            for (IpRecordManager.SameIpStats sameIpStats : entry.getValue()) {
                hoverText = hoverText.append(Utils.coloredMessage("&6%s (%s) &f| &6%d次\n".formatted(sameIpStats.getIp(), sameIpStats.getIpLocation(), sameIpStats.getCount())));
                totalCount += sameIpStats.getCount();
            }

            Component temp = Utils.coloredMessage("&6%s &f| &6使用相同ip登录次数：%d\n".formatted(historyNamesStr, totalCount));
            temp = temp.hoverEvent(HoverEvent.showText(hoverText));
            treeMap.put(totalCount, temp);
        }
        for (Map.Entry<Integer, Component> entry : treeMap.entrySet()) {
            dupeipOutput = dupeipOutput.append(entry.getValue());
        }
        source.sendMessage(dupeipOutput);
        source.sendMessage(Utils.coloredMessage("&f========================================================="));
    }

    private @NotNull String getHistoryNamesStr(String playerNameOrUuid) {
        List<String> playerHistoryNames;
        if (playerNameOrUuid.length() == 36 && playerNameOrUuid.contains("-")) {
            // uuid
            playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByUuid(playerNameOrUuid);
            if (bannedPlayersUuid.contains(playerNameOrUuid)) {
                playerNameOrUuid = "&c" + playerNameOrUuid;
            }
        } else {
            playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByName(playerNameOrUuid);
            if (bannedPlayersUuid.contains(plugin.getIpRecordManager().getPlayerUuidByName(playerNameOrUuid))) {
                playerNameOrUuid = "&c" + playerNameOrUuid;
            }
        }

        if (!playerHistoryNames.isEmpty() && bannedPlayersUuid.contains(plugin.getIpRecordManager().getPlayerUuidByName(playerHistoryNames.getLast()))) {
            String lastName = playerHistoryNames.getLast();
            playerHistoryNames.removeLast();
            playerHistoryNames.addLast("&c" + lastName);
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
        } else {
            List<String> suggest = Utils.getAllPlayerName();
            if (invocation.arguments().length > 0 && invocation.arguments()[0].trim().equals("dupeip")) {
                suggest.addFirst("--range");
            }
            return suggest;
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bciptool");
    }
}
