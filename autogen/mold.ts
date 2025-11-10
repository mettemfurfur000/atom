// generate_molds.ts
import {stringify} from "yaml";

type MoldDef = {
    id: string;
    display: string; // for translations
};

const baseTexture = "minecraft:item/ceramic/"
const unfiredBaseTexture = "minecraft:item/ceramic/unfired_"
const firedBaseTexture = "minecraft:item/ceramic/fired/"

// Add or remove mold defs as needed
const MOLDS = [
    {id: "axe_head", display: "Axe Head"},
    {id: "hammer_head", display: "Hammer Head"},
    {id: "ingot", display: "Ingot"},
    {id: "knife_blade", display: "Knife Blade"},
    {id: "pickaxe_head", display: "Pickaxe Head"},
    {id: "saw_blade", display: "Saw Blade"},
    {id: "shovel_head", display: "Shovel Head"},
    {id: "sword_blade", display: "Sword Blade"},
] as const satisfies MoldDef[];

type Mold = typeof MOLDS[number];

type MoldId = (typeof MOLDS)[number]["id"];

// Item variants
type MoldVariant = "clay" | "fired";

const CATEGORY_KEY = "atom:molds";

// Helper: build item key like atom:clay_mold_knife or atom:fired_mold_knife
function itemKey(id: MoldId, variant: MoldVariant) {
    return `atom:${variant}_mold_${id}`;
}

// Helper: texture path by variant
function textureFor(def: Mold, variant: MoldVariant) {
    return variant === "clay" ? unfiredBaseTexture + def.id : firedBaseTexture + def.id + "_empty";
}

// Common item data builder
function buildItem(def: Mold, variant: MoldVariant) {
    const key = itemKey(def.id, variant);
    const material = variant === "clay" ? "clay_ball" : "brick"; // adjust if needed
    const l10nKey = `<!i><white><lang:item.mold.${variant}.${def.id}.name>`;
    const lore = [
        "<!i><gray><lang:item.mold.common.lore>", // generic line; set translation
        variant === "clay" ? "<!i><gray><lang:item.mold.unfired.lore>" : null,
        "",
        `<!i><gray><lang:item.mold.${def.id}.lore>`,
        "",
        variant === "clay"
            ? "<!i><white><image:atom:badge_material> <image:atom:badge_age_copper>"
            : "<!i><white><image:atom:badge_utility> <image:atom:badge_age_copper>",
    ];

    return {
        [key]: {
            material,
            data: {
                "item-name": l10nKey,
                lore: lore.filter(a => a !== null),
            },
            model: {
                template: "default:model/simplified_generated",
                arguments: {
                    path: textureFor(def, variant),
                },
            },
        },
    };
}

function generateDoc() {
    const items: Record<string, unknown> = {};
    const list: string[] = [];
    const variants: MoldVariant[] = ["clay", "fired"];

    // Build items
    for (const def of MOLDS) {
        for (const v of variants) {
            Object.assign(items, buildItem(def, v));
            list.push(itemKey(def.id, v));
        }
    }

    // Category
    const categories = {
        [CATEGORY_KEY]: {
            name: "<!i><white><lang:category.molds.name></white>",
            hidden: true,
            lore: ["<!i><gray><lang:category.molds.lore>"],
            icon: "atom:clay_mold_shovel_head", // change to a valid icon id you have
            list,
        },
    };

    // Translations
    const en: Record<string, string> = {
        "category.molds.name": "Clay and Fired Molds",
        "category.molds.lore": "Reusable molds for shaping tools and parts",
        "item.mold.common.lore": "A mold used in early crafting",
        "item.mold.unfired.lore": "Fire this in a kiln to create a mold",
        "item.mold.fired.lore": "Fired mold",
    };

    for (const def of MOLDS) {
        en[`item.mold.clay.${def.id}.name`] = `Clay ${def.display} Mold`;
        en[`item.mold.fired.${def.id}.name`] = `Fired ${def.display} Mold`;
        en[`item.mold.${def.id}.lore`] = `Used to make a ${def.display}`;
    }

    return {
        items,
        categories,
        lang: {en},
    };
}

const yaml = stringify(generateDoc(), {lineWidth: 0});
await Bun.write("../run/plugins/CraftEngine/resources/atom/configuration/auto/molds.yml", yaml);
console.log("Generated molds.yml (items + category + translations)");