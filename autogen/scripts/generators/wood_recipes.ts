// scripts/generators/wood_recipes.ts
import {asYamlDoc, atom, mc, type AnyRecipe, ShapelessRecipe} from "../lib/itemkit";
import {OUT} from "../lib/paths";
import type {Generator} from "./types";

const WOOD_TYPES = [
    "oak",
    "spruce",
    "birch",
    "jungle",
    "acacia",
    "dark_oak",
    "cherry",
    "pale_oak",
    "mangrove",
] as const;
type WoodType = (typeof WOOD_TYPES)[number];

function recipeKey(wood: WoodType) {
    return atom(`saw_${wood}_planks`);
}

function logId(wood: WoodType) {
    // vanilla naming is <wood>_log for these types
    return woodSuffixed(wood, "log");
}

function strippedLogId(wood: WoodType) {
    return mc(`stripped_${wood}_log`);
}

function saplingId(wood: WoodType) {
    return woodSuffixed(wood, "sapling");
}

function woodId(wood: WoodType) {
    return woodSuffixed(wood, "wood");
}

function strippedWoodId(wood: WoodType) {
    return mc(`stripped_${wood}_wood`);
}

function planksId(wood: WoodType) {
    return woodSuffixed(wood, "planks")
}

function slabId(wood: WoodType) {
    return woodSuffixed(wood, "slab");
}

function stairsId(wood: WoodType) {
    return woodSuffixed(wood, "stairs");
}

function doorId(wood: WoodType) {
    return woodSuffixed(wood, "door");
}

function trapdoorId(wood: WoodType) {
    return woodSuffixed(wood, "trapdoor");
}

function woodSuffixed(wood: WoodType, suffix: string) {
    return mc(`${wood}_${suffix}`);
}

function buildShaped<I extends string>(pattern: string[], ingredients: Record<I, string>, output: string, amount: number = 1): AnyRecipe {
    return {
        type: "shaped",
        pattern: pattern,
        ingredients: ingredients,
        result: {
            id: output,
            count: amount,
        },
    };
}

function buildShapeless(ingredients: string[], output: string, amount: number = 1): ShapelessRecipe {
    return {
        type: "shapeless",
        ingredients: ingredients,
        result: {
            id: output,
            count: amount,
        },
    };
}

function buildSawRecipe(input: string, output: string, pattern: string[] = ["S", "I"], amount: number = 2): AnyRecipe {
    return {
        type: "shaped",
        pattern: pattern,
        ingredients: {
            S: "#" + atom("tool_saw"),
            I: input,
        },
        result: {
            id: output,
            count: amount,
        },
    };
}


function buildType(wood: WoodType, suffix: string, natural: boolean) {
    const key = `minecraft:${wood}_${suffix}`;
    const lore = [
        "",
        natural ? `<!i><white><image:atom:badge_natural> <image:atom:badge_age_foraging>` : `<!i><white><image:atom:badge_material> <image:atom:badge_age_foraging>`,
    ];
    return {
        [key]: {
            "client-bound-data": {lore},
        },
    };
}

function generateWoodRecipes() {
    const recipes: Record<string, AnyRecipe> = {};
    for (const wood of WOOD_TYPES) {
        recipes[`atom:${wood}_log_to_planks`] = buildSawRecipe(logId(wood), planksId(wood));
        recipes[`atom:${wood}_stripped_log_to_planks`] = buildSawRecipe(strippedLogId(wood), planksId(wood));
        recipes[`atom:${wood}_log_to_stripped`] = buildShaped(
            ["A", "L"],
            {A: "#atom:tool_axe", L: strippedLogId(wood)},
            `minecraft:${wood}_stripped_log`,
            1
        );
        recipes[`atom:${wood}_planks_to_slab`] = buildSawRecipe(planksId(wood), slabId(wood));
        recipes[`atom:${wood}_planks_to_stairs`] = buildSawRecipe(planksId(wood), stairsId(wood), ["SI", "II"], 4);
        recipes[`atom:${wood}_fence`] = buildShaped(["SPS", " P ", "SPS"], {
            P: slabId(wood),
            S: 'atom:stick_bundle'
        }, `minecraft:${wood}_fence`, 6);
        recipes[`atom:${wood}_fence_gate`] = buildShaped(["PSP", "PSP", "PSP"], {
            P: slabId(wood),
            S: 'atom:stick_bundle'
        }, `minecraft:${wood}_fence_gate`, 2);
        recipes[`atom:${wood}_door`] = buildShaped(["PP", "PP", "PP"], {P: slabId(wood)}, `minecraft:${wood}_door`, 3);
        recipes[`atom:${wood}_trapdoor`] = buildShaped(["PPP", "PPP"], {P: slabId(wood)}, `minecraft:${wood}_trapdoor`, 3);
        recipes[`atom:${wood}_button`] = buildShapeless([planksId(wood)], `minecraft:${wood}_button`)
    }
    return {recipes};
}

function generateWoodItems() {
    const items: Record<string, unknown> = {};
    for (const wood of WOOD_TYPES) {
        Object.assign(items, buildType(wood, "log", true));
        Object.assign(items, buildType(wood, "planks", false));
        Object.assign(items, buildType(wood, "sapling", true));
    }
    return {items};
}

export const generateWoodRecipesFile: Generator = () => {
    const doc = generateWoodRecipes();
    const yaml = asYamlDoc(doc);

    return {
        writes: [{path: OUT.woodRecipes, content: yaml}, {
            path: OUT.root + "/wood_items.yml", content: asYamlDoc(generateWoodItems())
        }]
    };
};