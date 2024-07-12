package com.bilicraft.biliwhitelistvelocity.common;

import com.google.common.collect.ImmutableList;
import org.enginehub.squirrelid.Profile;
import org.enginehub.squirrelid.resolver.HttpRepositoryService;
import org.enginehub.squirrelid.resolver.ProfileService;
import org.enginehub.squirrelid.util.HttpRequest;
import org.enginehub.squirrelid.util.UUIDs;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpRepositoryServicePatched extends HttpRepositoryService {
    private final int maxRetries = 5;
    private final long retryDelay = 50L;
    private static final Logger log = Logger.getLogger(HttpRepositoryService.class.getCanonicalName());
    public HttpRepositoryServicePatched(String agent) {
        super(agent);
    }
    public static ProfileService forMinecraft() {
        return new HttpRepositoryServicePatched("Minecraft");
    }
    @Override
    protected ImmutableList<Profile> queryByName(Iterable<String> names) throws IOException, InterruptedException {
        List<Profile> profiles = new ArrayList<>();
        int retriesLeft = this.maxRetries;
        long retryDelay = this.retryDelay;

        Object result;
        for (String name : names) {
            while(true) {
                try {
                    result = HttpRequest.get(new URL("https://api.mojang.com/users/profiles/minecraft/"+name)).execute().returnContent().asJson();
                    if(result instanceof Map){
                        Map jsonObject= (Map) result;
                        if(!jsonObject.containsKey("errorMessage")){
                            try {
                                profiles.add(new Profile(UUID.fromString(UUIDs.addDashes(String.valueOf(jsonObject.get("id")))), String.valueOf(jsonObject.get("name"))));
                            }catch (IllegalArgumentException | ClassCastException var6) {
                                log.log(Level.WARNING, "Got invalid value from UUID lookup service", var6);
                            }
                        }
                    }
                    break;
                } catch (IOException var10) {
                    if (retriesLeft == 0) {
                        throw var10;
                    }

                    log.log(Level.WARNING, "Failed to query profile service -- retrying...", var10);
                    Thread.sleep(retryDelay);
                    retryDelay *= 2L;
                    --retriesLeft;
                }
            }
        }

        return ImmutableList.copyOf(profiles);
    }
    @Override
    protected ImmutableList<Profile> queryByUuid(Iterable<UUID> uuids) throws IOException, InterruptedException {
        List<Profile> profiles = new ArrayList<>();
        int retriesLeft = this.maxRetries;
        long retryDelay = this.retryDelay;

        for (UUID uuid : uuids) {
            while (true) {
                try {
                    Object result = HttpRequest.get(HttpRequest.url("https://sessionserver.mojang.com/session/minecraft/profile/"+UUIDs.stripDashes(uuid.toString()))).execute().returnContent().asJson();

                        try {
                            if (result instanceof Map) {
                                Map<Object, Object> mapEntry = (Map)result;
                                Object rawName = mapEntry.get("name");
                                if (rawName != null) {
                                    String name = String.valueOf(rawName);
                                    profiles.add(new Profile(uuid, name));
                                }
                            }
                        } catch (IllegalArgumentException | ClassCastException var5) {
                            log.log(Level.WARNING, "Got invalid value from Name History lookup service", var5);
                        }
                    break;
                } catch (IOException e) {
                    if (retriesLeft == 0) {
                        throw e;
                    }
                    log.log(Level.WARNING, "Failed to query name history service -- retrying...", e);
                    Thread.sleep(retryDelay);
                    retryDelay *= 2L;
                    --retriesLeft;
                }
            }
        }

        return ImmutableList.copyOf(profiles);
    }
}
