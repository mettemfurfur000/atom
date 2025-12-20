package org.shotrush.atom.content.mobs.ai.debug;

public enum DebugLevel {
    OFF,
    MINIMAL,
    NORMAL,
    VERBOSE;
    
    public boolean isEnabled() {
        return this != OFF;
    }
    
    public boolean isAtLeast(DebugLevel level) {
        return this.ordinal() >= level.ordinal();
    }
}
