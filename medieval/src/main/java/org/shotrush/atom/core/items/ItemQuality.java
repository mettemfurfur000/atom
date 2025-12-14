package org.shotrush.atom.core.items;

import lombok.Getter;

@Getter
public enum ItemQuality {
    LOW("§7Low Quality", 0.5, 0.5, 1.5),
    MEDIUM("§fMedium Quality", 0.75, 0.75, 1.2),
    HIGH("§aHigh Quality", 1.0, 1.0, 1.0);
    
    private final String displayName;
    private final double efficiencyMultiplier;
    private final double durabilityMultiplier;
    private final double processingCostMultiplier;
    
    ItemQuality(String displayName, double efficiencyMultiplier, double durabilityMultiplier, double processingCostMultiplier) {
        this.displayName = displayName;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.durabilityMultiplier = durabilityMultiplier;
        this.processingCostMultiplier = processingCostMultiplier;
    }
    
    public static ItemQuality fromTemperature(double temperature) {
        if (temperature < 15.0) {
            return LOW;
        } else if (temperature > 25.0) {
            return LOW;
        } else {
            double deviation = Math.abs(temperature - 20.0);
            if (deviation <= 2.0) {
                return HIGH;
            } else {
                return MEDIUM;
            }
        }
    }
    
    public static ItemQuality average(ItemQuality... qualities) {
        if (qualities == null || qualities.length == 0) return MEDIUM;
        
        int totalValue = 0;
        int count = 0;
        
        for (ItemQuality quality : qualities) {
            if (quality != null) {
                totalValue += quality.ordinal();
                count++;
            }
        }
        
        if (count == 0) return MEDIUM;
        
        double average = (double) totalValue / count;
        
        if (average < 0.5) return LOW;
        if (average > 1.5) return HIGH;
        return MEDIUM;
    }
}
