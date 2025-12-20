package org.shotrush.atom.content.mobs.herd;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Animals;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DominanceHierarchy {
    
    private final Map<UUID, DominanceRank> memberRanks;
    private final Map<UUID, Integer> confrontationWins;
    private final Herd herd;
    
    private static final double ALPHA_THRESHOLD = 0.85;
    private static final double BETA_THRESHOLD = 0.65;
    private static final double SUBORDINATE_THRESHOLD = 0.30;
    
    public DominanceHierarchy(Herd herd) {
        this.herd = herd;
        this.memberRanks = new ConcurrentHashMap<>();
        this.confrontationWins = new ConcurrentHashMap<>();
    }
    
    public DominanceRank getRank(UUID memberId) {
        return memberRanks.getOrDefault(memberId, DominanceRank.SUBORDINATE);
    }
    
    public void setRank(UUID memberId, DominanceRank rank) {
        memberRanks.put(memberId, rank);
    }
    
    public void recalculateRanks() {
        Set<UUID> members = herd.members();
        if (members.size() <= 1) {
            if (!members.isEmpty()) {
                UUID leaderId = members.iterator().next();
                memberRanks.put(leaderId, DominanceRank.ALPHA);
            }
            return;
        }
        
        List<RankedMember> rankings = new ArrayList<>();
        
        for (UUID memberId : members) {
            Animals animal = (Animals) Bukkit.getEntity(memberId);
            if (animal == null || !animal.isValid()) continue;
            
            double score = calculateDominanceScore(animal, memberId);
            rankings.add(new RankedMember(memberId, score));
        }
        
        rankings.sort(Comparator.comparingDouble(RankedMember::score).reversed());
        
        int totalMembers = rankings.size();
        for (int i = 0; i < rankings.size(); i++) {
            UUID memberId = rankings.get(i).memberId();
            double position = (double) i / Math.max(1, totalMembers - 1);
            
            DominanceRank rank;
            if (position <= ALPHA_THRESHOLD && i == 0) {
                rank = DominanceRank.ALPHA;
            } else if (position <= BETA_THRESHOLD && i <= 1) {
                rank = DominanceRank.BETA;
            } else if (position >= 1.0 - SUBORDINATE_THRESHOLD && i == rankings.size() - 1) {
                rank = DominanceRank.OMEGA;
            } else {
                rank = DominanceRank.SUBORDINATE;
            }
            
            memberRanks.put(memberId, rank);
        }
    }
    
    private double calculateDominanceScore(Animals animal, UUID memberId) {
        double healthRatio = animal.getHealth() / 
            Objects.requireNonNull(animal.getAttribute(Attribute.MAX_HEALTH)).getValue();
        
        long age = animal.getTicksLived();
        double ageScore = Math.min(1.0, age / 480000.0);
        
        int wins = confrontationWins.getOrDefault(memberId, 0);
        double winScore = Math.min(1.0, wins / 10.0);
        
        boolean isLeader = herd.leader().equals(memberId);
        double leaderBonus = isLeader ? 0.3 : 0.0;
        
        return (healthRatio * 0.4) + (ageScore * 0.3) + (winScore * 0.2) + leaderBonus;
    }
    
    public void recordConfrontationWin(UUID winnerId) {
        confrontationWins.merge(winnerId, 1, Integer::sum);
        recalculateRanks();
    }
    
    public void challengeForRank(UUID challengerId, UUID defenderId) {
        Animals challenger = (Animals) Bukkit.getEntity(challengerId);
        Animals defender = (Animals) Bukkit.getEntity(defenderId);
        
        if (challenger == null || defender == null) return;
        
        double challengerScore = calculateDominanceScore(challenger, challengerId);
        double defenderScore = calculateDominanceScore(defender, defenderId);
        
        if (challengerScore > defenderScore * 1.2) {
            recordConfrontationWin(challengerId);
        } else {
            recordConfrontationWin(defenderId);
        }
    }
    
    public boolean hasHigherRank(UUID member1, UUID member2) {
        DominanceRank rank1 = getRank(member1);
        DominanceRank rank2 = getRank(member2);
        return rank1.ordinal() < rank2.ordinal();
    }
    
    public boolean canAccessResource(UUID memberId, UUID competitorId) {
        return hasHigherRank(memberId, competitorId);
    }
    
    public boolean canMate(UUID memberId) {
        DominanceRank rank = getRank(memberId);
        return rank == DominanceRank.ALPHA || rank == DominanceRank.BETA;
    }
    
    public void removeMember(UUID memberId) {
        memberRanks.remove(memberId);
        confrontationWins.remove(memberId);
        recalculateRanks();
    }
    
    private record RankedMember(UUID memberId, double score) {}
}
