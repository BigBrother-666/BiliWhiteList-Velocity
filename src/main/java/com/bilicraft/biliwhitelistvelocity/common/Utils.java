package com.bilicraft.biliwhitelistvelocity.common;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Utils {
    public static TextComponent coloredMessage(String msg) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(msg);
    }

    public static boolean boolFromInt(int i){
        return i != 0;
    }

    public static int boolToInt(boolean bool) {
        if (bool)
            return 1;
        return 0;
    }

    public static void broadcast(String content) {
        BiliWhiteListVelocity.instance.getServer().sendMessage(coloredMessage(content));
    }

    public static List<String> getAllPlayerName(){
        Collection<Player> allPlayers = BiliWhiteListVelocity.instance.getServer().getAllPlayers();
        List<String> allPlayerName = new ArrayList<>();
        for (Player player : allPlayers) {
            allPlayerName.add(player.getUsername());
        }
        return allPlayerName;
    }
}
