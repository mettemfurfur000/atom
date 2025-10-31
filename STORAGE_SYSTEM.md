# Storage System

Simple YAML-based storage for player and server data.

## File Structure

```
plugins/Atom/
├── serverdata.yml
└── playerdata/
    ├── <uuid>.yml
    ├── <uuid>.yml
    └── ...
```

## Usage

### Player Data

```java
DataStorage storage = Atom.getInstance().getDataStorage();
UUID uuid = player.getUniqueId();

YamlConfiguration config = storage.getPlayerData(uuid);
config.set("stats.kills", 10);
config.set("stats.deaths", 5);
config.set("inventory.saved", true);
storage.savePlayerData(uuid, config);

int kills = config.getInt("stats.kills", 0);
```

### Server Data

```java
DataStorage storage = Atom.getInstance().getDataStorage();

YamlConfiguration config = storage.getServerData();
config.set("settings.pvp", true);
config.set("current_age", "bronze_age");
storage.saveServerData(config);

boolean pvp = config.getBoolean("settings.pvp", false);
```

## API Methods

```java
storage.getPlayerData(uuid)           
storage.savePlayerData(uuid, config)  
storage.hasPlayerData(uuid)           
storage.deletePlayerData(uuid)        

storage.getServerData()               
storage.saveServerData(config)        
```

## Age Persistence

The Age system automatically saves to `serverdata.yml`:

```yaml
current_age: bronze_age
```

When the server restarts, the current age is automatically loaded.

## Example: Custom Player Stats

```java
public void savePlayerStats(Player player) {
    DataStorage storage = Atom.getInstance().getDataStorage();
    YamlConfiguration config = storage.getPlayerData(player.getUniqueId());
    
    config.set("stats.kills", config.getInt("stats.kills", 0) + 1);
    config.set("stats.last_login", System.currentTimeMillis());
    config.set("location.x", player.getLocation().getX());
    config.set("location.y", player.getLocation().getY());
    config.set("location.z", player.getLocation().getZ());
    
    storage.savePlayerData(player.getUniqueId(), config);
}
```

## Example: Server Configuration

```java
public void loadServerSettings() {
    DataStorage storage = Atom.getInstance().getDataStorage();
    YamlConfiguration config = storage.getServerData();
    
    boolean pvpEnabled = config.getBoolean("settings.pvp", true);
    String difficulty = config.getString("settings.difficulty", "normal");
    int maxPlayers = config.getInt("settings.max_players", 100);
    
    applySettings(pvpEnabled, difficulty, maxPlayers);
}
```
