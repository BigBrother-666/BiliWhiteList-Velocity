package com.bilicraft.biliwhitelistvelocity.listeners;

import com.bilicraft.biliwhitelistvelocity.BiliWhiteListVelocity;
import litebans.api.Entry;
import litebans.api.Events;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LiteBansListener extends Events.Listener {
    private final BiliWhiteListVelocity plugin;
    public LiteBansListener(BiliWhiteListVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void entryRemoved(Entry entry) {
        switch (entry.getType()) {
            case "ban":
                // This is an unban event.
                break;
            case "mute":
                // This is an unmute event.
                break;
            case "warn":
                // This is an unwarn event.
                break;
        }
    }

    @Override
    public void entryAdded(Entry entry) {
        switch (entry.getType()) {
            case "ban":
                // This is a ban event.
                break;
            case "mute":
                // This is a mute event.
                break;
            case "warn":
                // This is a warn event.
                break;
            case "kick":
                // This is a kick event.
                break;
        }
    }

    @Override
    public void broadcastSent(@NotNull String message, @Nullable String type) {

    }
}

