package com.bilicraft.biliwhitelistvelocity.Database;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;

import java.io.File;
import java.sql.Connection;

public class IpRecordDatabase {
    private final HikariDataSource ds;

    public IpRecordDatabase(BiliWhiteListVelocity plugin) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataDirectory().toString() + File.separator + "ip_record.db");
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        this.ds = new HikariDataSource(config);
    }

    @SneakyThrows
    public Connection getConnection() {
        return ds.getConnection();
    }
}
