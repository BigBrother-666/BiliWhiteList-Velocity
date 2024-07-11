package com.bilicraft.biliwhitelistvelocity.Database;

public class Utils {
    public static boolean boolFromInt(int i){
        return i != 0;
    }

    public static int boolToInt(boolean bool){
        if(bool)
            return 1;
        return 0;
    }
}
