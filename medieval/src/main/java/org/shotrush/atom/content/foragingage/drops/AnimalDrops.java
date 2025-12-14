package org.shotrush.atom.content.foragingage.drops;

import org.shotrush.atom.core.blocks.annotation.CustomEntityDrops;
import org.shotrush.atom.core.blocks.annotation.CustomEntityDrops.Drop;
import org.shotrush.atom.core.blocks.annotation.CustomEntityDrops.EntityCategory;

@CustomEntityDrops(
    categories = {
        EntityCategory.ANIMALS
    },
    drops = {
        @Drop(customItemId = "bone", minAmount = 1, maxAmount = 3),
        @Drop(customItemId = "uncured_leather", minAmount = 1, maxAmount = 5)
    },
    replaceVanillaDrops = true,
    ages = {"foraging_age"}
)
public class AnimalDrops {
}
