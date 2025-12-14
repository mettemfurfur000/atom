package org.shotrush.atom.content.mobs.herd;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Herd {
    
    private final UUID id;
    private final EntityType species;
    private final World world;
    private final Set<UUID> members;
    private UUID leader;
    private long panicUntil;
    private Location lastThreatLocation;
    private final DominanceHierarchy dominanceHierarchy;
    
    public Herd(UUID id, EntityType species, World world, UUID initialLeader) {
        this.id = id;
        this.species = species;
        this.world = world;
        this.members = ConcurrentHashMap.newKeySet();
        this.leader = initialLeader;
        this.panicUntil = 0;
        this.lastThreatLocation = null;
        this.dominanceHierarchy = new DominanceHierarchy(this);
        this.members.add(initialLeader);
    }
    
    public UUID id() {
        return id;
    }
    
    public EntityType species() {
        return species;
    }
    
    public World world() {
        return world;
    }
    
    public Set<UUID> members() {
        return members;
    }
    
    public UUID leader() {
        return leader;
    }
    
    public void setLeader(UUID leader) {
        this.leader = leader;
    }
    
    public boolean isPanicking() {
        return System.currentTimeMillis() < panicUntil;
    }
    
    public void setPanic(long durationMs, Location threatLocation) {
        this.panicUntil = System.currentTimeMillis() + durationMs;
        this.lastThreatLocation = threatLocation;
    }
    
    public Location lastThreatLocation() {
        return lastThreatLocation;
    }
    
    public void addMember(UUID memberId) {
        members.add(memberId);
    }
    
    public void removeMember(UUID memberId) {
        members.remove(memberId);
    }
    
    public int size() {
        return members.size();
    }
    
    public boolean isEmpty() {
        return members.isEmpty();
    }
    
    public DominanceHierarchy getDominanceHierarchy() {
        return dominanceHierarchy;
    }
}
