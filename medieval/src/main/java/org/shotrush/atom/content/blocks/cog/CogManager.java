package org.shotrush.atom.content.blocks.cog;

import org.bukkit.plugin.Plugin;
import org.shotrush.atom.core.blocks.CustomBlock;
import org.shotrush.atom.core.blocks.util.BlockLocationUtil;
import org.shotrush.atom.core.blocks.util.BlockNetworkUtil;

import java.util.*;

public class CogManager {
    
    private final Plugin plugin;
    
    public CogManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void recalculatePower(List<CustomBlock> allBlocks) {
        List<Cog> cogs = BlockNetworkUtil.filterBlocks(allBlocks, Cog.class);

        plugin.getLogger().info("=== RECALCULATING POWER ===");
        
        
        for (Cog cog : cogs) {
            if (!cog.isPowerSource()) {
                cog.setPowered(false);
                cog.setRotationDirection(1);
            } else {
                cog.setRotationDirection(1);
            }
        }

        Set<Cog> visited = new HashSet<>();
        Queue<CogPowerInfo> queue = new LinkedList<>();

        
        for (Cog cog : cogs) {
            if (cog.isPowerSource()) {
                queue.add(new CogPowerInfo(cog, 1));
                visited.add(cog);
                cog.setPowered(true);
                cog.setRotationDirection(1);
            }
        }

        
        while (!queue.isEmpty()) {
            CogPowerInfo current = queue.poll();
            Cog currentCog = current.cog;
            int currentDirection = current.direction;
            
            List<Cog> adjacent = BlockLocationUtil.getAdjacentBlocks(
                currentCog.getBlockLocation(), 
                allBlocks, 
                Cog.class
            );
            
            for (Cog adj : adjacent) {
                if (!visited.contains(adj)) {
                    visited.add(adj);
                    adj.setPowered(true);
                    
                    int newDirection = calculateRotationDirection(currentCog, adj, currentDirection);
                    adj.setRotationDirection(newDirection);
                    queue.add(new CogPowerInfo(adj, newDirection));
                }
            }
        }
        
        plugin.getLogger().info("=== POWER CALCULATION COMPLETE ===");
    }
    
    private int calculateRotationDirection(Cog current, Cog adjacent, int currentDirection) {
        boolean sameAxis = current.isSameAxisAs(adjacent);
        boolean alongAxis = current.isConnectedAlongAxis(adjacent);
        
        if (sameAxis && alongAxis) {
            return currentDirection;
        } else if (sameAxis && !alongAxis) {
            return -currentDirection;
        } else {
            return currentDirection;
        }
    }

    private static class CogPowerInfo {
        Cog cog;
        int direction;
        
        CogPowerInfo(Cog cog, int direction) {
            this.cog = cog;
            this.direction = direction;
        }
    }
}
