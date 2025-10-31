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
    
    private static final String BLACK_SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTYxMzU4Mzg3MjQyMCwKICAicHJvZmlsZUlkIiA6ICI0ZWQ4MjMzNzFhMmU0YmI3YTVlYWJmY2ZmZGE4NDk1NyIsCiAgInByb2ZpbGVOYW1lIiA6ICJGYWJyaWNhdGlvbiIsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zZjQ3NmVhNzc0ZGE0ZWE1ZmQ0MWVlNGRkNjUxOWE1MjEyMzRhYmM2ZDIzMjgzZjU2ZTk3ZTc3ZjA2YjI0YWQwIgogICAgfQogIH0KfQ==";
    private static final String BLACK_SKIN_SIGNATURE = "";
    public static void setPlayerSkin(Player player, String value, String signature) {
        PlayerProfile profile = player.getPlayerProfile();
        profile.getProperties().removeIf(prop -> prop.getName().equals("textures"));
        if (signature != null && !signature.isEmpty()) {
            profile.setProperty(new ProfileProperty("textures", value, signature));
        } else {
            profile.setProperty(new ProfileProperty("textures", value));
        }
        player.setPlayerProfile(profile);
    }
    
    public static void setDefaultBlackSkin(Player player) {
        setPlayerSkin(player, BLACK_SKIN_VALUE, BLACK_SKIN_SIGNATURE);
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
