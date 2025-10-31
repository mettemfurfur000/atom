package org.shotrush.atom.core.blocks.util;

import org.shotrush.atom.core.blocks.CustomBlock;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;


public class BlockNetworkUtil {
    
    
    public static <T extends CustomBlock> void breadthFirstSearch(
            List<CustomBlock> allBlocks,
            Class<T> blockType,
            Predicate<T> isSource,
            BiConsumer<T, Integer> onVisit) {
        
        List<T> blocks = filterBlocks(allBlocks, blockType);
        Set<T> visited = new HashSet<>();
        Queue<BlockDepthPair<T>> queue = new LinkedList<>();
        
        
        for (T block : blocks) {
            if (isSource.test(block)) {
                queue.add(new BlockDepthPair<>(block, 0));
                visited.add(block);
            }
        }
        
        
        while (!queue.isEmpty()) {
            BlockDepthPair<T> current = queue.poll();
            onVisit.accept(current.block, current.depth);
            
            List<T> adjacent = BlockLocationUtil.getAdjacentBlocks(
                current.block.getBlockLocation(), 
                allBlocks, 
                blockType
            );
            
            for (T adj : adjacent) {
                if (!visited.contains(adj)) {
                    visited.add(adj);
                    queue.add(new BlockDepthPair<>(adj, current.depth + 1));
                }
            }
        }
    }
    
    
    public static <T extends CustomBlock, S> void breadthFirstSearchWithState(
            List<CustomBlock> allBlocks,
            Class<T> blockType,
            Predicate<T> isSource,
            Function<T, S> getInitialState,
            TriConsumer<T, S, List<T>> onVisit) {
        
        List<T> blocks = filterBlocks(allBlocks, blockType);
        Set<T> visited = new HashSet<>();
        Queue<BlockStatePair<T, S>> queue = new LinkedList<>();
        
        
        for (T block : blocks) {
            if (isSource.test(block)) {
                S initialState = getInitialState.apply(block);
                queue.add(new BlockStatePair<>(block, initialState));
                visited.add(block);
            }
        }
        
        
        while (!queue.isEmpty()) {
            BlockStatePair<T, S> current = queue.poll();
            
            List<T> adjacent = BlockLocationUtil.getAdjacentBlocks(
                current.block.getBlockLocation(), 
                allBlocks, 
                blockType
            );
            
            onVisit.accept(current.block, current.state, adjacent);
            
            
            for (T adj : adjacent) {
                if (!visited.contains(adj)) {
                    visited.add(adj);
                }
            }
        }
    }
    
    
    public static <T extends CustomBlock> List<T> filterBlocks(
            List<CustomBlock> allBlocks, 
            Class<T> blockType) {
        
        List<T> filtered = new ArrayList<>();
        for (CustomBlock block : allBlocks) {
            if (blockType.isInstance(block)) {
                filtered.add(blockType.cast(block));
            }
        }
        return filtered;
    }
    
    
    public static <T extends CustomBlock> Set<T> findConnectedNetwork(
            T startBlock,
            List<CustomBlock> allBlocks,
            Class<T> blockType) {
        
        Set<T> network = new HashSet<>();
        Queue<T> queue = new LinkedList<>();
        
        queue.add(startBlock);
        network.add(startBlock);
        
        while (!queue.isEmpty()) {
            T current = queue.poll();
            
            List<T> adjacent = BlockLocationUtil.getAdjacentBlocks(
                current.getBlockLocation(), 
                allBlocks, 
                blockType
            );
            
            for (T adj : adjacent) {
                if (!network.contains(adj)) {
                    network.add(adj);
                    queue.add(adj);
                }
            }
        }
        
        return network;
    }
    
    
    private static class BlockDepthPair<T> {
        final T block;
        final int depth;
        
        BlockDepthPair(T block, int depth) {
            this.block = block;
            this.depth = depth;
        }
    }
    
    private static class BlockStatePair<T, S> {
        final T block;
        final S state;
        
        BlockStatePair(T block, S state) {
            this.block = block;
            this.state = state;
        }
    }
    
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
