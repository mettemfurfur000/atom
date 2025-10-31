package org.shotrush.atom.core.age;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.shotrush.atom.core.storage.DataStorage;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgeManager {
    
    private final Plugin plugin;
    private final DataStorage storage;
    private final Map<String, Age> ages = new ConcurrentHashMap<>();
    private final List<Age> orderedAges = new ArrayList<>();
    
    @Getter
    private Age currentAge;
    
    public AgeManager(Plugin plugin, DataStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        loadCurrentAge();
    }
    
    public void registerAge(@NotNull Age age) {
        ages.put(age.getId(), age);
        orderedAges.add(age);
        orderedAges.sort(Comparator.comparingInt(Age::getOrder));
    }
    
    public void registerAges(@NotNull Age... ages) {
        for (Age age : ages) {
            registerAge(age);
        }
    }
    
    public Optional<Age> getAge(String id) {
        return Optional.ofNullable(ages.get(id));
    }
    
    public List<Age> getAllAges() {
        return Collections.unmodifiableList(orderedAges);
    }
    
    public Optional<Age> getNextAge() {
        if (currentAge == null) {
            return orderedAges.isEmpty() ? Optional.empty() : Optional.of(orderedAges.get(0));
        }
        
        int currentIndex = orderedAges.indexOf(currentAge);
        if (currentIndex < 0 || currentIndex >= orderedAges.size() - 1) {
            return Optional.empty();
        }
        
        return Optional.of(orderedAges.get(currentIndex + 1));
    }
    
    public Optional<Age> getPreviousAge() {
        if (currentAge == null) {
            return Optional.empty();
        }
        
        int currentIndex = orderedAges.indexOf(currentAge);
        if (currentIndex <= 0) {
            return Optional.empty();
        }
        
        return Optional.of(orderedAges.get(currentIndex - 1));
    }
    
    public boolean progressToAge(@NotNull String ageId, @Nullable Player trigger) {
        return getAge(ageId).map(age -> progressToAge(age, trigger)).orElse(false);
    }
    
    public boolean progressToAge(@NotNull Age newAge, @Nullable Player trigger) {
        AgeProgressEvent event = new AgeProgressEvent(currentAge, newAge, trigger);
        event.callEvent();
        
        if (event.isCancelled()) {
            return false;
        }
        
        Age previousAge = currentAge;
        currentAge = newAge;
        
        saveCurrentAge();
        broadcastAgeChange(previousAge, newAge, trigger);
        
        return true;
    }
    
    public boolean progressToNextAge(@Nullable Player trigger) {
        return getNextAge().map(age -> progressToAge(age, trigger)).orElse(false);
    }
    
    public void setAge(@NotNull Age age) {
        this.currentAge = age;
        saveCurrentAge();
    }
    
    public void setAge(@NotNull String ageId) {
        getAge(ageId).ifPresent(this::setAge);
    }
    
    private void saveCurrentAge() {
        if (currentAge == null) {
            return;
        }
        
        YamlConfiguration config = storage.getServerData();
        config.set("current_age", currentAge.getId());
        storage.saveServerData(config);
    }
    
    private void loadCurrentAge() {
        YamlConfiguration config = storage.getServerData();
        String ageId = config.getString("current_age");
        
        if (ageId != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                getAge(ageId).ifPresent(age -> {
                    this.currentAge = age;
                    plugin.getLogger().info("Loaded current age: " + age.getDisplayName());
                });
            }, 1L);
        }
    }
    
    private void broadcastAgeChange(@Nullable Age previousAge, @NotNull Age newAge, @Nullable Player trigger) {
        Title title = Title.title(
            newAge.getTitleComponent(),
            newAge.getSubtitleComponent(),
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(3),
                Duration.ofSeconds(1)
            )
        );
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
        
        Component announcement = newAge.getAnnouncementComponent();
        if (trigger != null) {
            announcement = announcement.append(Component.text(" (Triggered by " + trigger.getName() + ")"));
        }
        
        Bukkit.broadcast(announcement);
    }
    
    public void broadcastCurrentAge() {
        if (currentAge == null) {
            return;
        }
        
        broadcastAgeChange(null, currentAge, null);
    }
    
    public boolean canProgress() {
        return getNextAge().isPresent();
    }
    
    public boolean isInAge(@NotNull String ageId) {
        return currentAge != null && currentAge.getId().equals(ageId);
    }
    
    public boolean isInAge(@NotNull Age age) {
        return currentAge != null && currentAge.equals(age);
    }
    
    public int getCurrentAgeIndex() {
        return currentAge == null ? -1 : orderedAges.indexOf(currentAge);
    }
    
    public int getTotalAges() {
        return orderedAges.size();
    }
}
