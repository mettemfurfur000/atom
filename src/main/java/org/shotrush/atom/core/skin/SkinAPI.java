package org.shotrush.atom.core.skin;

import com.destroystokyo.paper.profile.ProfileProperty;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class SkinAPI {
    private static final String DEFAULT_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc2MTk1Mjg2NTgyNSwKICAicHJvZmlsZUlkIiA6ICIzOWQ4ZWJhY2NmZmU0ZTA4YTRiMWQ4NmQ4YWIwNGM4OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJBemRhcmlhaCIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9jOTYwZWNiYTQ4MzljY2JiYzAwM2NkYjMyYTViYzJiMmFmNjdlZDlmNzZjYmE1OGY3NWE1MDkxNDlmNGIwMjQzIiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=";
    private static final String DEFAULT_SKIN_SIGNATURE = "";

    public static void setPlayerSkin(Player player, String value, String signature) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
        if (signature != null && !signature.isEmpty()) {
            profile.setProperty(new ProfileProperty("textures", value, signature));
        } else {
            profile.setProperty(new ProfileProperty("textures", value));
        }
        player.setPlayerProfile(profile);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.hidePlayer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")), player);
                online.getScheduler().runDelayed(
                        Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")),
                    task -> online.showPlayer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")), player),
                    null,
                    2L
                );
            }
        }
    }
    
    public static void setDefaultSkin(Player player) {
        setPlayerSkin(player, DEFAULT_SKIN_VALUE, DEFAULT_SKIN_SIGNATURE);
    }
    
    public static CompletableFuture<Boolean> setSkinFromUsername(Player player, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI uri0 = URI.create("https://api.mojang.com/users/profiles/minecraft/" + username);
                InputStreamReader reader0 = new InputStreamReader(uri0.toURL().openStream());
                String uuid = JsonParser.parseReader(reader0).getAsJsonObject().get("id").getAsString();
                reader0.close();
                
                URI uri1 = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false");
                InputStreamReader reader1 = new InputStreamReader(uri1.toURL().openStream());
                JsonObject properties = JsonParser.parseReader(reader1).getAsJsonObject()
                    .get("properties").getAsJsonArray().get(0).getAsJsonObject();
                reader1.close();
                
                String value = properties.get("value").getAsString();
                String signature = properties.get("signature").getAsString();
                
                player.getScheduler().run(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")), task -> {
                    setPlayerSkin(player, value, signature);
                }, null);
                
                return true;
            } catch (IOException | NullPointerException e) {
                return false;
            }
        });
    }
    
    public static CompletableFuture<Boolean> setSkinFromURL(Player player, String textureURL) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String value = java.util.Base64.getEncoder().encodeToString(
                    ("{\"textures\":{\"SKIN\":{\"url\":\"" + textureURL + "\"}}}").getBytes()
                );
                
                player.getScheduler().run(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Atom")), task -> {
                    setPlayerSkin(player, value, "");
                }, null);
                
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }
}
