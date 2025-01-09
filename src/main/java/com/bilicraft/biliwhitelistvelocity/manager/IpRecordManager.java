package com.bilicraft.biliwhitelistvelocity.manager;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.bilicraft.biliwhitelistvelocity.config.Config;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import litebans.api.Database;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class IpRecordManager {
    private final BiliWhiteListVelocity plugin;
    private final String recordTableName = "ip_record";
    private final Cache<String, String> locCache;

    public IpRecordManager(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
        this.locCache = CacheBuilder.newBuilder().maximumSize(200).build();

        // 创建记录表
        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); Statement statement = connection.createStatement()) {
            String sql = """
                    CREATE TABLE IF NOT EXISTS %s (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                login_time DATETIME,
                                player_uuid VARCHAR(36),
                                player_name VARCHAR(36),
                                ip VARCHAR(64),
                                ip_location VARCHAR(128)
                    );
                    """.formatted(recordTableName);
            statement.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
    }

    public void addRecord(String playerName, UUID uuid, String ip) {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            try (Connection connection = plugin.getIpRecordDatabase().getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + recordTableName + "` (`login_time`, `player_uuid`, `player_name`, `ip`, `ip_location`) VALUES (?, ?, ?, ?, ?)");
                preparedStatement.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                preparedStatement.setString(2, uuid.toString());
                preparedStatement.setString(3, playerName);
                preparedStatement.setString(4, ip);
                preparedStatement.setString(5, getIpLocation(ip));
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().error(e.toString());
            }
        }).schedule();
    }

    /**
     * @param ip ip地址
     * @return ip归属地
     */
    @Nullable
    private String getIpLocation(String ip) {
        if (!(Boolean) Config.getAssociatedAccountConf().getOrDefault("db-record-loc", true)) {
            return null;
        }
        String cache = locCache.getIfPresent(ip);
        if (cache != null) {
            return cache;
        }
        synchronized (this) {
            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(((String) Config.getJointLiabilityConf().getOrDefault("loc-api", "https://api.ip.sb/geoip/{ip}")).replace("{ip}", ip)))
                        .header("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:133.0) Gecko/20100101 Firefox/133.0")
                        .build();

                for (int i = 0; i < 3; i++) {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        Gson gson = new Gson();
                        LocJsonResp respJson = gson.fromJson(response.body(), LocJsonResp.class);
                        String loc = "%s-%s-%s".formatted(respJson.country, respJson.region, respJson.city);
                        locCache.put(ip, loc);
                        return loc;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warn(e.toString());
            }
        }
        return "未知-未知-未知";
    }

    /**
     * 通过玩家名字查询玩家 UUID
     *
     * @param playerName 玩家名
     * @return 玩家 UUID
     */
    @Nullable
    private String getPlayerUuidByName(String playerName) {
        String sql = "SELECT DISTINCT player_uuid FROM ip_record WHERE player_name = ?";
        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("player_uuid");
            } else {
                return null;
            }
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
            return null;
        }
    }

    /**
     * 通过 UUID 查询曾用名
     *
     * @param playerUuid 玩家uuid
     * @return 曾用名列表
     */
    public List<String> getPlayerHistoryNamesByUuid(String playerUuid) {
        List<String> names = new ArrayList<>();
        String sql = "SELECT DISTINCT player_name FROM ip_record WHERE player_uuid = ? ORDER BY login_time";

        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString("player_name"));
            }
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
        return names;
    }

    private List<IpLocationInfo> getPlayerIplocationinfoById(String playerUuid, String ipLoc) {
        String sql = "SELECT ip, COUNT(*) AS count FROM ip_record WHERE player_uuid = ? AND ip_location = ? GROUP BY ip ORDER BY count";
        List<IpLocationInfo> info = new ArrayList<>();

        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, ipLoc);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // 查询该ip的登录时间
                sql = "SELECT MIN(login_time) AS first_login_time, MAX(login_time) AS last_login_time FROM ip_record WHERE player_uuid = ? AND ip = ?";
                PreparedStatement stmt2 = connection.prepareStatement(sql);
                stmt2.setString(1, playerUuid);
                stmt2.setString(2, rs.getString("ip"));
                ResultSet rs2 = stmt2.executeQuery();
                if (rs2.next()) {
                    info.add(new IpLocationInfo(rs.getString("ip"), rs.getInt("count"), rs2.getString("first_login_time"), rs2.getString("last_login_time")));
                }
                stmt2.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
        return info;
    }

    /**
     * 通过玩家名查找曾用名
     *
     * @param playerName 玩家名
     * @return 曾用名列表
     */
    public List<String> getPlayerHistoryNamesByName(String playerName) {
        String playerUuid = getPlayerUuidByName(playerName);
        if (playerUuid == null) {
            return new ArrayList<>();
        }
        return getPlayerHistoryNamesByUuid(playerUuid);
    }

    /**
     * 查找和某玩家使用相同ip登陆过的玩家
     *
     * @param playerNameOrUuid 玩家名或uuid
     * @return 使用相同ip登录的玩家列表
     */
    public Map<String, ArrayList<SameIpStats>> getPlayersWithSameIP(String playerNameOrUuid, int range) {
        Map<String, ArrayList<SameIpStats>> sameIpPlayers = new HashMap<>();
        String playerUuid;
        if (playerNameOrUuid.length() == 36 && playerNameOrUuid.contains("-")) {
            playerUuid = playerNameOrUuid;
        } else {
            playerUuid = getPlayerUuidByName(playerNameOrUuid);
        }
        String sql;
        if (range > 0) {
            sql = "SELECT player_uuid, ip, ip_location, COUNT(*) AS count FROM ip_record WHERE ip IN (SELECT ip FROM ip_record WHERE player_uuid = ?) AND player_uuid != ? AND login_time >= DATETIME('now', '- ? days') GROUP BY ip, player_uuid";
        } else {
            sql = "SELECT player_uuid, ip, ip_location, COUNT(*) AS count FROM ip_record WHERE ip IN (SELECT ip FROM ip_record WHERE player_uuid = ?) AND player_uuid != ? GROUP BY ip, player_uuid";
        }
        if (playerUuid == null) {
            return sameIpPlayers;
        }

        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, playerUuid);
            if (range > 0) {
                stmt.setInt(3, range);
            }
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("player_uuid");
                if (sameIpPlayers.containsKey(uuid)) {
                    sameIpPlayers.get(uuid).add(new SameIpStats(rs.getString("ip_location"), rs.getString("ip"), rs.getInt("count")));
                } else {
                    ArrayList<SameIpStats> temp = new ArrayList<>();
                    temp.add(new SameIpStats(rs.getString("ip_location"), rs.getString("ip"), rs.getInt("count")));
                    sameIpPlayers.put(uuid, temp);
                }
                Collections.sort(sameIpPlayers.get(uuid));
            }
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
        return sameIpPlayers;
    }

    /**
     * 统计某玩家的登录ip属地占比
     *
     * @param playerName 玩家名
     * @return ip属地统计
     */
    public List<IpLocationStats> getPlayerIpLocationRatio(String playerName) {
        String sql = "SELECT ip_location, COUNT(*) AS count FROM ip_record WHERE player_uuid = ? GROUP BY ip_location";
        List<IpLocationStats> locationStats = new ArrayList<>();
        String playerUuid = getPlayerUuidByName(playerName);
        if (playerUuid == null) {
            return locationStats;
        }

        try (Connection connection = plugin.getIpRecordDatabase().getConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String ipLocation = rs.getString("ip_location");
                int count = rs.getInt("count");
                locationStats.add(new IpLocationStats(ipLocation, count, getPlayerIplocationinfoById(playerUuid, ipLocation)));
            }

            Collections.sort(locationStats);
            return locationStats;
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
        return locationStats;
    }

    /**
     * 获取当前所有封禁玩家
     * @return 封禁玩家uuid列表
     */
    public List<String> getBannedPlayersUuid() {
        List<String> bannedPlayers = new ArrayList<>();
        String query = "SELECT DISTINCT uuid FROM {bans} WHERE active=1 AND (until < 1 OR until > ?)";
        try (PreparedStatement st = Database.get().prepareStatement(query)) {
            st.setLong(1, Instant.now().toEpochMilli());
            ResultSet rs = st.executeQuery();
            bannedPlayers.add(rs.getString("uuid"));
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }

        return bannedPlayers;
    }

    /**
     * dupeip指令使用
     */
    @Data
    @AllArgsConstructor
    public static class SameIpStats implements Comparable<SameIpStats> {
        private String ipLocation;
        private String ip;
        private int count;

        @Override
        public int compareTo(@NotNull IpRecordManager.SameIpStats o) {
            return Integer.compare(o.count, this.count);
        }
    }

    /**
     * iphistory指令使用
     */
    @Data
    @AllArgsConstructor
    public static class IpLocationStats implements Comparable<IpLocationStats> {
        private String ipLocation;
        private int count;
        private List<IpLocationInfo> info;

        @Override
        public int compareTo(@NotNull IpRecordManager.IpLocationStats o) {
            return Integer.compare(o.count, this.count);
        }

        public static int getTotalLogin(List<IpLocationStats> data) {
            int sum = 0;
            for (IpLocationStats stats : data) {
                sum += stats.count;
            }
            return sum;
        }
    }

    @Data
    @AllArgsConstructor
    public static class IpLocationInfo {
        private String ip;
        private int count;
        private String firstLoginTIme;
        private String lastLoginTIme;
    }

    @Data
    public static class LocJsonResp {
        private String country = "未知";
        private String region = "未知";
        private String city = "未知";
    }
}
