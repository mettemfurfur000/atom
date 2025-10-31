package org.shotrush.atom.core.age;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.shotrush.atom.Atom;

public class AgeExample {
    
    public static void setupDefaultAges(AgeManager manager) {
        Age stoneAge = Age.builder()
                .id("stone_age")
                .displayName("Stone")
                .year(10000)
                .isBC(true)
                .order(0)
                .titleColor(TextColor.color(128, 128, 128))
                .description("The beginning of civilization")
                .build();
        
        Age bronzeAge = Age.builder()
                .id("bronze_age")
                .displayName("Bronze")
                .year(3000)
                .isBC(true)
                .order(1)
                .titleColor(TextColor.color(205, 127, 50))
                .description("Metal working begins")
                .build();
        
        Age ironAge = Age.builder()
                .id("iron_age")
                .displayName("Iron")
                .year(1200)
                .isBC(true)
                .order(2)
                .titleColor(TextColor.color(192, 192, 192))
                .description("Stronger metals discovered")
                .build();
        
        Age medievalAge = Age.builder()
                .id("medieval_age")
                .displayName("Medieval")
                .year(500)
                .isBC(false)
                .order(3)
                .titleColor(TextColor.color(139, 69, 19))
                .description("Castles and kingdoms")
                .build();
        
        Age industrialAge = Age.builder()
                .id("industrial_age")
                .displayName("Industrial")
                .year(1760)
                .isBC(false)
                .order(4)
                .titleColor(TextColor.color(70, 70, 70))
                .description("Machines and factories")
                .build();
        
        Age modernAge = Age.builder()
                .id("modern_age")
                .displayName("Modern")
                .year(1945)
                .isBC(false)
                .order(5)
                .titleColor(TextColor.color(0, 191, 255))
                .description("Technology and innovation")
                .build();
        
        manager.registerAges(stoneAge, bronzeAge, ironAge, medievalAge, industrialAge, modernAge);
        manager.setAge(stoneAge);
    }
    
    public static void progressToNextAge(Player trigger) {
        AgeManager manager = Atom.getInstance().getAgeManager();
        
        if (manager.progressToNextAge(trigger)) {
            trigger.sendMessage("You have advanced the server to the next age!");
        } else {
            trigger.sendMessage("Cannot progress to next age - already at the final age!");
        }
    }
    
    public static void progressToAge(String ageId, Player trigger) {
        AgeManager manager = Atom.getInstance().getAgeManager();
        
        if (manager.progressToAge(ageId, trigger)) {
            trigger.sendMessage("Age changed successfully!");
        } else {
            trigger.sendMessage("Failed to change age - age not found or event cancelled!");
        }
    }
    
    public static void checkCurrentAge(Player player) {
        AgeManager manager = Atom.getInstance().getAgeManager();
        Age current = manager.getCurrentAge();
        
        if (current == null) {
            player.sendMessage("No age is currently set!");
            return;
        }
        
        player.sendMessage("Current Age: " + current.getDisplayName());
        player.sendMessage("Year: " + current.getYearDisplay());
        player.sendMessage("Progress: " + (manager.getCurrentAgeIndex() + 1) + "/" + manager.getTotalAges());
        
        manager.getNextAge().ifPresentOrElse(
            next -> player.sendMessage("Next Age: " + next.getDisplayName() + " (" + next.getYearDisplay() + ")"),
            () -> player.sendMessage("This is the final age!")
        );
    }
}
