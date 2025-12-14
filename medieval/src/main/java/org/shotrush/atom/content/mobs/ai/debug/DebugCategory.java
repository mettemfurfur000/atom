package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.text.format.TextColor;

public enum DebugCategory {
    GOALS("Goals", TextColor.color(85, 255, 85)),
    NEEDS("Needs", TextColor.color(255, 170, 0)),
    MEMORY("Memory", TextColor.color(170, 85, 255)),
    COMBAT("Combat", TextColor.color(255, 85, 85)),
    SOCIAL("Social", TextColor.color(85, 170, 255)),
    ENVIRONMENTAL("Environmental", TextColor.color(255, 255, 85));
    
    private final String displayName;
    private final TextColor color;
    
    DebugCategory(String displayName, TextColor color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public TextColor getColor() {
        return color;
    }
}
