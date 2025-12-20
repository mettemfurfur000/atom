package org.shotrush.atom.core.age;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class AgeProgressEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLER_LIST = new HandlerList();
    
    private final Age previousAge;
    private final Age newAge;
    private final Player trigger;
    
    @Setter
    private boolean cancelled = false;
    
    public AgeProgressEvent(@Nullable Age previousAge, @NotNull Age newAge, @Nullable Player trigger) {
        this.previousAge = previousAge;
        this.newAge = newAge;
        this.trigger = trigger;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
