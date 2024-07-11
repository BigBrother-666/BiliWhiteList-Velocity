package com.bilicraft.biliwhitelistvelocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import org.slf4j.Logger;

@Plugin(
        id = "biliwhitelist-velocity",
        name = "BiliWhiteList-Velocity",
        version = "1.0-SNAPSHOT",
        authors = {"BigBrother"}
)
public class BiliWhiteList_Velocity {

    @Inject
    private Logger logger;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }
}
