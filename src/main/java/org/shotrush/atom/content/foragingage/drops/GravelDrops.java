package org.shotrush.atom.content.foragingage.drops;

import org.bukkit.Material;
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops;
import org.shotrush.atom.core.blocks.annotation.CustomBlockDrops.Drop;

@CustomBlockDrops(
    blocks = {
        Material.GRAVEL
    },
    drops = {
        @Drop(material = Material.FLINT, chance = 0.45, min = 1, max = 1),
        @Drop(customItemId = "sharpened_rock", chance = 0.05, min = 1, max = 1)
    },
    replaceVanillaDrops = true,
    ages = {"foraging_age"}
)
public class GravelDrops {
}
