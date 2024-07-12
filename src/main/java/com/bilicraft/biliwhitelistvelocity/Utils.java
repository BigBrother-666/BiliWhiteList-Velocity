package com.bilicraft.biliwhitelistvelocity;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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
}
