# Age Progression System

Server-wide progression system with automatic notifications.

## Quick Start

### Define Ages

```java
Age stoneAge = Age.builder()
    .id("stone_age")
    .displayName("Stone")
    .year(10000)
    .isBC(true)
    .order(0)
    .titleColor(TextColor.color(128, 128, 128))
    .description("The beginning")
    .build();

Age modernAge = Age.builder()
    .id("modern_age")
    .displayName("Modern")
    .year(1945)
    .isBC(false)
    .order(5)
    .titleColor(TextColor.color(0, 191, 255))
    .build();
```

### Register Ages

```java
AgeManager manager = Atom.getInstance().getAgeManager();
manager.registerAges(stoneAge, bronzeAge);
manager.setAge(stoneAge);
```

### Progress to Next Age

```java
manager.progressToNextAge(player);
```

This automatically:
- Shows title to all players: "The Modern Age" / "Year: 1945 AD"
- Broadcasts: "We are in the Modern age now. Year: 1945 AD"
- Fires `AgeProgressEvent` for other plugins

## API Methods

### AgeManager

```java
manager.registerAge(age)                    
manager.progressToAge("bronze_age", player) 
manager.progressToNextAge(player)           
manager.getCurrentAge()                     
manager.getNextAge()                        
manager.getAllAges()                        
manager.isInAge("stone_age")                
manager.canProgress()                       
manager.broadcastCurrentAge()               
```

### Age Properties

```java
age.getId()                 
age.getDisplayName()        
age.getYear()               
age.isBC()                  
age.getYearDisplay()        
age.getOrder()              
age.getTitleColor()         
age.getDescription()        
age.isAfter(otherAge)       
age.isBefore(otherAge)      
```

## Events

Listen to age changes:

```java
@EventHandler
public void onAgeProgress(AgeProgressEvent event) {
    Age newAge = event.getNewAge();
    Age previousAge = event.getPreviousAge();
    Player trigger = event.getTrigger();
    
    if (newAge.getId().equals("industrial_age")) {
        // Unlock machines
    }
    
    // Cancel progression
    event.setCancelled(true);
}
```

## Commands

- `/age` or `/age info` - View current age
- `/age next` - Progress to next age
- `/age set <id>` - Set specific age
- `/age list` - List all ages
- `/age broadcast` - Re-broadcast current age

## Permissions

- `atom.age.progress` - Progress to next age
- `atom.age.set` - Set specific age
- `atom.age.broadcast` - Broadcast age

## Example: Trigger on Action

```java
@EventHandler
public void onCraft(CraftItemEvent event) {
    ItemStack result = event.getRecipe().getResult();
    
    if (result.getType() == Material.IRON_INGOT) {
        AgeManager manager = Atom.getInstance().getAgeManager();
        
        if (manager.isInAge("bronze_age")) {
            manager.progressToAge("iron_age", (Player) event.getWhoClicked());
        }
    }
}
```
