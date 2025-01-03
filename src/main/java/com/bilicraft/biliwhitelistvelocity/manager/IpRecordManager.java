package com.bilicraft.biliwhitelistvelocity.manager;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Country;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class IpRecordManager {
    private final BiliWhiteListVelocity plugin;
    private final String recordTableName = "ip_record";
    private File database;

    public IpRecordManager(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;

        // 加载ip属地数据库
        File directory = new File(plugin.getDataDirectory().toString());
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".mmdb")) {
                    plugin.getLogger().info("加载mmdb文件：{}", file.getAbsolutePath());
                    this.database = file;
                    break;
                }
            }
        }

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
        try (Connection connection = plugin.getIpRecordDatabase().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + recordTableName + "` (`login_time`, `player_uuid`, `player_name`, `ip`, `ip_location`) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setObject(1, LocalDateTime.now());
            preparedStatement.setString(2, uuid.toString());
            preparedStatement.setString(3, playerName);
            preparedStatement.setString(4, ip);
            preparedStatement.setString(5, getIpAddress(ip));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().error(e.toString());
        }
    }

    /**
     * @param ip ip地址
     * @return ip归属地
     */
    public String getIpAddress(String ip) {
        if (database == null) {
            return "未知";
        }
        try (DatabaseReader reader = new DatabaseReader.Builder(database).build()) {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);
            Country country = response.getCountry();
            return country.getNames().get("zh-CN") != null ? country.getNames().get("zh-CN") : country.getName();
        } catch (IOException e) {
            BiliWhiteListVelocity.instance.getLogger().error(e.toString());
            return "未知";
        } catch (GeoIp2Exception e) {
            BiliWhiteListVelocity.instance.getLogger().warn(e.toString());
            return "未知";
        }
    }
}
