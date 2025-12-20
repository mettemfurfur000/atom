package org.shotrush.atom.content.foragingage.drops;

import org.bukkit.Material;
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops;
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops.Drop;

@CustomBlockDrops(
    blockPattern = "_LEAVES",  
    drops = {
        @Drop(material = Material.VINE, chance = 0.15, min = 1, max = 2),
        @Drop(material = Material.APPLE, chance = 0.05, min = 1, max = 1),
        @Drop(material = Material.OAK_SAPLING, chance = 0.05, min = 1, max = 1)
    },
    replaceVanillaDrops = true,
    ages = {"foraging_age"}  
)
public class LeafDrops {
}
