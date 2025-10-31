package org.shotrush.atom.core.age;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

@Getter
@Builder
public class Age {
    
    @NonNull
    private final String id;
    
    @NonNull
    private final String displayName;
    
    @Builder.Default
    private final int year = 0;
    
    @Builder.Default
    private final boolean isBC = true;
    
    @Builder.Default
    private final int order = 0;
    
    @Builder.Default
    private final TextColor titleColor = TextColor.color(255, 215, 0);
    
    @Builder.Default
    private final TextColor subtitleColor = TextColor.color(200, 200, 200);
    
    private final String description;
    
    public String getYearDisplay() {
        return Math.abs(year) + " " + (isBC ? "BC" : "AD");
    }
    
    public Component getTitleComponent() {
        return Component.text("The " + displayName + " Age").color(titleColor);
    }
    
    public Component getSubtitleComponent() {
        return Component.text("Year: " + getYearDisplay()).color(subtitleColor);
    }
    
    public Component getAnnouncementComponent() {
        return Component.text("We are in the ")
                .append(Component.text(displayName).color(titleColor))
                .append(Component.text(" age now. Year: " + getYearDisplay()));
    }
    
    public boolean isAfter(Age other) {
        return this.order > other.order;
    }
    
    public boolean isBefore(Age other) {
        return this.order < other.order;
    }
}
