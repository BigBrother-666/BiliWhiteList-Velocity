package com.bilicraft.biliwhitelistvelocity.config;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.Yaml;

public class Config {
    @Getter
    private static Map<String, Object> config;
    private static Path configFile;

    @Getter @Setter
    private static boolean enabled;

    /**
     * 加载配置文件
     */
    public static void loadConfig(BiliWhiteListVelocity biliWhiteList) {
        configFile = Path.of(biliWhiteList.getDataDirectory() + "/config.yml");

        // 创建配置路径
        if(!biliWhiteList.getDataDirectory().toFile().exists()) {
            biliWhiteList.getDataDirectory().toFile().mkdir();
        }

        // 如果配置文件不存在，复制默认配置文件
        Yaml yaml = new Yaml();
        if(!configFile.toFile().exists()) {
            try (InputStream inputStream = BiliWhiteListVelocity.class.getResourceAsStream("/config.yml")) {
                Files.copy(inputStream, configFile);
            } catch (Exception e) {
                biliWhiteList.getLogger().error("config.yml初始化失败");
                biliWhiteList.getLogger().error(e.getMessage());
            }
        }
        // 加载配置到内存
         try (InputStream inputStream = new FileInputStream(configFile.toFile())) {
             config = yaml.load(inputStream);
         } catch (Exception e) {
             biliWhiteList.getLogger().error("config.yml加载失败");
             biliWhiteList.getLogger().error(e.getMessage());
        }
    }
}
