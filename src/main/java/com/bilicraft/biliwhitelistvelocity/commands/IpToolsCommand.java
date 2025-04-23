package com.bilicraft.biliwhitelistvelocity.commands;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.common.Utils;
import com.bilicraft.biliwhitelistvelocity.manager.IpRecordManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IpToolsCommand implements SimpleCommand {
    private final BiliWhiteListVelocity plugin;
    private List<String> bannedPlayersUuid;
    private static ScheduledTask updateTask;
    private volatile boolean shouldStopTask = false;

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
                        String playerUuid = plugin.getIpRecordManager().getPlayerUuidByName(args[1]);
                        if (playerUuid == null) {
                            source.sendMessage(Utils.coloredMessage("&e没有查询到 %s &e的登录信息".formatted(args[1])));
                            return;
                        }
                        Map<String, ArrayList<IpRecordManager.SameIpStats>> players = plugin.getIpRecordManager().getPlayersWithSameIP(playerUuid, range);
                        subcommandDupeip(playerUuid, source, players);
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

                    String playerUuid = plugin.getIpRecordManager().getPlayerUuidByName(args[1]);
                    if (playerUuid == null) {
                        source.sendMessage(Utils.coloredMessage("&e没有查询到 %s &e的登录信息".formatted(args[1])));
                        return;
                    }
                    Component iphistoryOutput = Utils.coloredMessage("&6%s &a的登录信息（指针移动到各行可查看详情）：\n".formatted(getHistoryNamesStr(playerUuid)));
                    // 查询
                    List<IpRecordManager.IpLocationStats> playerIpLocationRatio = plugin.getIpRecordManager().getPlayerIpLocationRatio(playerUuid, range);

                    if (playerIpLocationRatio.isEmpty()) {
                        source.sendMessage(Utils.coloredMessage("&e没有查询到 %s &e的登录信息".formatted(getHistoryNamesStr(playerUuid))));
                        return;
                    }

                    int total = IpRecordManager.IpLocationStats.getTotalLogin(playerIpLocationRatio);
                    for (IpRecordManager.IpLocationStats ipLocationStats : playerIpLocationRatio) {
                        // 生成悬浮文字
                        Component hoverText = Utils.coloredMessage("&a玩家 %s &a登录属地 %s 的ip详情：\n&f-------------------------------------------\n".formatted(getHistoryNamesStr(playerUuid), ipLocationStats.getIpLocation()));
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
                case "updateplayer":
                    if (args.length < 3) {
                        source.sendMessage(Utils.coloredMessage("&c缺少参数 /bciptool updateplayer <playername> <start/stop>"));
                        return;
                    }
                    if (args[2].equals("start")) {
                        if (updateTask != null) {
                            source.sendMessage(Utils.coloredMessage("&e已有进行中的任务"));
                            return;
                        }
                        String uuid = plugin.getIpRecordManager().getPlayerUuidByName(args[1]);
                        if (uuid == null) {
                            source.sendMessage(Utils.coloredMessage("&c玩家不存在"));
                            return;
                        }
                        List<String> ipList = plugin.getIpRecordManager().getPlayerLoginIp(uuid);
                        source.sendMessage(Utils.coloredMessage("&a查询到玩家 %s 的 %d 个登录ip，开始更新登录属地...".formatted(args[1], ipList.size())));
                        int innerRange = range;
                        updateTask = plugin.getServer().getScheduler().buildTask(plugin, () -> {
                            int updateCnt = 0;
                            for (String ip : ipList) {
                                if (shouldStopTask) {
                                    source.sendMessage(Utils.coloredMessage("&a任务停止成功，共更新了 %s 条数据".formatted(updateCnt)));
                                    shouldStopTask = false;
                                    updateTask = null;
                                    return;
                                }
                                updateCnt += plugin.getIpRecordManager().updateLoc(ip, innerRange);
                            }
                            source.sendMessage(Utils.coloredMessage("&aip属地更新完成，共更新了 %s 条数据".formatted(updateCnt)));
                            if (source instanceof Player) {
                                plugin.getServer().getConsoleCommandSource().sendMessage(Utils.coloredMessage("&aip属地更新完成，共更新了 %s 条数据".formatted(updateCnt)));
                            }
                            updateTask = null;
                        }).schedule();
                    } else if (args[2].equals("stop")) {
                        if (updateTask == null) {
                            source.sendMessage(Utils.coloredMessage("&a任务不存在"));
                        } else {
                            shouldStopTask = true;
                        }
                    }
                    return;
                case "update":
                    if (args.length == 1) {
                        return;
                    }
                    if (args[1].equals("start")) {
                        if (range <= 0) {
                            source.sendMessage(Utils.coloredMessage("&c必须指定range"));
                            return;
                        }
                        if (updateTask != null) {
                            source.sendMessage(Utils.coloredMessage("&e已有进行中的任务"));
                            return;
                        }
                        List<String> unknownLocIp = plugin.getIpRecordManager().getUnknownLocIp(range);
                        source.sendMessage(Utils.coloredMessage("&a查询到 %d 条没有属地的ip，开始更新...".formatted(unknownLocIp.size())));
                        int innerRange = range;
                        updateTask = plugin.getServer().getScheduler().buildTask(plugin, () -> {
                            int updateCnt = 0;
                            for (String ip : unknownLocIp) {
                                if (shouldStopTask) {
                                    source.sendMessage(Utils.coloredMessage("&a任务停止成功，共更新了 %s 条数据".formatted(updateCnt)));
                                    shouldStopTask = false;
                                    updateTask = null;
                                    return;
                                }
                                updateCnt += plugin.getIpRecordManager().updateLoc(ip, innerRange);
                            }
                            source.sendMessage(Utils.coloredMessage("&aip属地更新完成，共更新了 %s 条数据".formatted(updateCnt)));
                            if (source instanceof Player) {
                                plugin.getServer().getConsoleCommandSource().sendMessage(Utils.coloredMessage("&aip属地更新完成，共更新了 %s 条数据".formatted(updateCnt)));
                            }
                            updateTask = null;
                        }).schedule();
                    } else if (args[1].equals("stop")) {
                        if (updateTask == null) {
                            source.sendMessage(Utils.coloredMessage("&a任务不存在"));
                        } else {
                            shouldStopTask = true;
                        }
                    }
                    return;
                default:
                    source.sendMessage(Utils.coloredMessage("&c/bciptool dupeip [玩家ID] [--range <days>]: 查询某玩家关联的账号，不指定玩家名则查询所有已封禁玩家关联的账号，--range是可选参数，表示根据指定天数内的log查询。\n" +
                            "&c/bciptool iphistory <玩家ID> [--range <days>] : 查看某玩家的登录ip及属地统计信息"));
                    return;
            }
            source.sendMessage(Utils.coloredMessage("&b查询完成！正在封禁的玩家已用&c红色字体&b标出，白名单已回绝的玩家已用&4深红色字体&b标出。"));
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

    /**
     * dupeip核心逻辑
     */
    private void subcommandDupeip(String playerUuid, CommandSource source, Map<String, ArrayList<IpRecordManager.SameIpStats>> players) {
        Component dupeipOutput = Utils.coloredMessage("&a和 %s &a使用相同ip登录过的玩家（指针移动到玩家名上查看相同ip详情）：\n".formatted(getHistoryNamesStr(playerUuid)));
        if (players.isEmpty()) {
            source.sendMessage(Utils.coloredMessage("&e没有查询到和 %s &e使用相同ip登录过的玩家\n".formatted(getHistoryNamesStr(playerUuid))));
            source.sendMessage(Utils.coloredMessage("&f========================================================="));
            return;
        }

        TreeMap<Integer, Component> treeMap = new TreeMap<>(Comparator.reverseOrder());
        for (Map.Entry<String, ArrayList<IpRecordManager.SameIpStats>> entry : players.entrySet()) {
            String historyNamesStr = getHistoryNamesStr(entry.getKey());
            Component hoverText = Utils.coloredMessage("&6%s &a和 %s &a使用相同ip详情：\n&f-------------------------------------------\n".formatted(historyNamesStr, getHistoryNamesStr(playerUuid)));
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

    /**
     * @param playerUuid 玩家uuid
     * @return 玩家名和曾用名（如果有）输出格式字符串
     */
    private @NotNull String getHistoryNamesStr(String playerUuid) {
        // 获取玩家历史名列表
        List<String> playerHistoryNames = plugin.getIpRecordManager().getPlayerHistoryNamesByUuid(playerUuid);

        if (!playerHistoryNames.isEmpty()) {
            String uuid = plugin.getIpRecordManager().getPlayerUuidByName(playerHistoryNames.get(playerHistoryNames.size() - 1));
            String lastName = playerHistoryNames.get(playerHistoryNames.size() - 1);
            playerHistoryNames.remove(playerHistoryNames.size() - 1);
            if (bannedPlayersUuid.contains(uuid)) {
                playerHistoryNames.add("&c" + lastName);
            } else if (plugin.getIpRecordManager().isPlayerBlocked(uuid)) {
                playerHistoryNames.add("&4" + lastName);
            } else {
                playerHistoryNames.add("&6" + lastName);
            }
        }
        String historyNamesStr;
        if (playerHistoryNames.isEmpty()) {
            return playerUuid;
        } else if (playerHistoryNames.size() == 1) {
            historyNamesStr = playerHistoryNames.get(0);
        } else {
            String lastName = playerHistoryNames.get(playerHistoryNames.size() - 1);
            playerHistoryNames.remove(playerHistoryNames.size() - 1);
            historyNamesStr = lastName + "（曾用名：" + String.join(", ", playerHistoryNames) + "）";
        }
        return historyNamesStr;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            return List.of("dupeip", "iphistory");
        }

        if (args.length == 1) {
            return Stream.of("dupeip", "iphistory")
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if ((args.length == 2 || args.length == 3) && !args[args.length - 2].equals("--range")) {
            List<String> suggest = new ArrayList<>();
            if (args.length == 2) {
                suggest.addAll(plugin.getIpRecordManager().getAllPlayerName());
            }
            if (!(args[args.length - 2].equals("iphistory") && args.length == 2)) {
                suggest.add(0, "--range");
            }
            return suggest.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("biliwhitelist.bciptool");
    }
}
