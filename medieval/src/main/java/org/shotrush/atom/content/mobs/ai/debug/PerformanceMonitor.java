package org.shotrush.atom.content.mobs.ai.debug;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerformanceMonitor {
    
    private static final Map<String, PerformanceStats> stats = new ConcurrentHashMap<>();
    private static final int SAMPLE_SIZE = 100;
    
    private static class PerformanceStats {
        final String operation;
        final Queue<Long> samples;
        long totalExecutions;
        long totalTime;
        long minTime;
        long maxTime;
        
        PerformanceStats(String operation) {
            this.operation = operation;
            this.samples = new LinkedList<>();
            this.totalExecutions = 0;
            this.totalTime = 0;
            this.minTime = Long.MAX_VALUE;
            this.maxTime = 0;
        }
        
        synchronized void addSample(long nanos) {
            totalExecutions++;
            totalTime += nanos;
            minTime = Math.min(minTime, nanos);
            maxTime = Math.max(maxTime, nanos);
            
            samples.add(nanos);
            if (samples.size() > SAMPLE_SIZE) {
                samples.poll();
            }
        }
        
        synchronized double getAverageMs() {
            if (totalExecutions == 0) return 0;
            return (totalTime / (double) totalExecutions) / 1_000_000.0;
        }
        
        synchronized double getRecentAverageMs() {
            if (samples.isEmpty()) return 0;
            long sum = samples.stream().mapToLong(Long::longValue).sum();
            return (sum / (double) samples.size()) / 1_000_000.0;
        }
        
        synchronized double getMinMs() {
            return minTime == Long.MAX_VALUE ? 0 : minTime / 1_000_000.0;
        }
        
        synchronized double getMaxMs() {
            return maxTime / 1_000_000.0;
        }
    }
    
    public static void recordExecution(String operation, long nanos) {
        stats.computeIfAbsent(operation, PerformanceStats::new).addSample(nanos);
        
        double ms = nanos / 1_000_000.0;
        if (ms > 5.0) {
            DebugManager.log(
                String.format("SLOW OPERATION: %s took %.2fms", operation, ms),
                DebugCategory.GOALS,
                DebugLevel.MINIMAL
            );
        }
    }
    
    public static long startTracking(String operation) {
        return System.nanoTime();
    }
    
    public static void endTracking(String operation, long startTime) {
        long duration = System.nanoTime() - startTime;
        recordExecution(operation, duration);
    }
    
    public static void displayStats(Player player) {
        if (stats.isEmpty()) {
            player.sendMessage(Component.text("No performance data collected yet.", NamedTextColor.YELLOW));
            return;
        }
        
        player.sendMessage(Component.text("=== MobAI Performance Stats ===", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
        
        List<PerformanceStats> sorted = new ArrayList<>(stats.values());
        sorted.sort((a, b) -> Double.compare(b.getRecentAverageMs(), a.getRecentAverageMs()));
        
        for (PerformanceStats stat : sorted) {
            double avgMs = stat.getAverageMs();
            double recentAvgMs = stat.getRecentAverageMs();
            double minMs = stat.getMinMs();
            double maxMs = stat.getMaxMs();
            
            NamedTextColor color;
            if (recentAvgMs > 5.0) {
                color = NamedTextColor.RED;
            } else if (recentAvgMs > 2.0) {
                color = NamedTextColor.YELLOW;
            } else {
                color = NamedTextColor.GREEN;
            }
            
            player.sendMessage(Component.text(stat.operation, color));
            player.sendMessage(Component.text(String.format(
                "  Executions: %,d | Avg: %.2fms | Recent: %.2fms | Min: %.2fms | Max: %.2fms",
                stat.totalExecutions, avgMs, recentAvgMs, minMs, maxMs
            ), NamedTextColor.GRAY));
        }
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text(
            "Total tracked operations: " + stats.size(),
            NamedTextColor.GRAY
        ));
    }
    
    public static void reset() {
        stats.clear();
    }
    
    public static Map<String, Double> getAverages() {
        Map<String, Double> averages = new HashMap<>();
        for (Map.Entry<String, PerformanceStats> entry : stats.entrySet()) {
            averages.put(entry.getKey(), entry.getValue().getRecentAverageMs());
        }
        return averages;
    }
}
