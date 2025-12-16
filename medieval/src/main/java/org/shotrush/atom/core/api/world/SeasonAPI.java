package org.shotrush.atom.core.api.world;

import lombok.Getter;
import org.bukkit.World;

public class SeasonAPI {

    public enum Season {
        SPRING(0.0),
        SUMMER(5.0),
        AUTUMN(0.0),
        WINTER(-10.0);

        @Getter
        public final double tempModifier;

        Season(double tempModifier) {
            this.tempModifier = tempModifier;
        }
    }

    public static Season getCurrentSeason(World world) {
        // For now, we can base it on world time broadly or just default to SUMMER/SPRING
        // Or we could cycle based on total world time if we wanted.
        // Let's make it simple: Cycle every 30 in-game days?
        // For this overhaul, let's stick to a static season or simple calculation.
        
        long fullTime = world.getFullTime();
        long days = fullTime / 24000;
        int seasonIndex = (int) ((days / 30) % 4);
        
        return Season.values()[seasonIndex];
    }
}
