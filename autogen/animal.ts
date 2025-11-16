// generate_animal_products.ts
import {stringify, parse} from "yaml";
import { writeFileSync, readFileSync } from "fs";

const animals = [
    "cow",
    "pig",
    "sheep",
    "chicken",
    "rabbit",
    "horse",
    "donkey",
    "mule",
    "llama",
    "goat",
    "cat",
    "wolf",
    "fox",
    "panda",
    "polar_bear",
    "ocelot",
    "camel",
] as const;

type AnimalId = (typeof animals)[number];
type ItemType =
    | "raw_meat"
    | "undercooked_meat"
    | "cooked_meat"
    | "burnt_meat"
    | "raw_leather"
    | "leather"
    | "cured_leather"
    | "bone";

const CATEGORY_KEY = "atom:animal_product";
const CATEGORY = {
    name: "<!i><white><lang:category.animal_product.name></white>",
    hidden: true,
    lore: ["<!i><gray><lang:category.animal_product.lore>"],
    icon: "minecraft:leather",
    list: [] as string[],
};

// Display names for animals (used in translations)
const animalDisplay: Record<AnimalId, string> = {
    cow: "Cow",
    pig: "Pig",
    sheep: "Sheep",
    chicken: "Chicken",
    rabbit: "Rabbit",
    horse: "Horse",
    donkey: "Donkey",
    mule: "Mule",
    llama: "Llama",
    goat: "Goat",
    cat: "Cat",
    wolf: "Wolf",
    fox: "Fox",
    panda: "Panda",
    polar_bear: "Polar Bear",
    ocelot: "Ocelot",
    camel: "Camel",
};

const meatNameOverrides: Record<string, string> = {
    cow: "Beef",
    pig: "Pork",
    sheep: "Mutton",
    chicken: "Chicken",
    rabbit: "Rabbit",
};

const recipeTransitions = [
    { from: "raw_meat", to: "undercooked_meat", category: "food" },
    { from: "undercooked_meat", to: "cooked_meat", category: "food" },
    { from: "cooked_meat", to: "burnt_meat", category: "food" },
];
function itemKey(id: AnimalId, type: ItemType) {
    if (type.includes("leather") && type !== "leather") return `atom:animal_leather_${type.split("_")[0]}_${id}`;
    if (type.includes("meat")) return `atom:animal_meat_${type.split("_")[0]}_${id}`;
    return `atom:animal_${type}_${id}`;
}

function getTexturePathForType(type: ItemType) {
    switch (type) {
        case "raw_meat":
            return "minecraft:item/meat/raw";
        case "undercooked_meat":
            return "minecraft:item/meat/undercooked";
        case "cooked_meat":
            return "minecraft:item/meat/cooked";
        case "burnt_meat":
            return "minecraft:item/meat/burnt";
        case "raw_leather":
            return "minecraft:item/leather_meat";
        case "leather":
            return "minecraft:item/leather";
        case "cured_leather":
            return "minecraft:item/leather_cured";
        case "bone":
            return "minecraft:item/meat/bone";
        default:
            return "minecraft:item/redstone";
    }
}

function itemBlock(id: AnimalId, type: ItemType) {
    const baseMaterial = type.includes("leather") ? "leather" : type === "bone" ? "bone" : "beef";
    const itemNameKeySuffix = type; // e.g., raw_meat
    const texturePath = getTexturePathForType(type);
    const tag = type

    return {
        [itemKey(id, type)]: {
            material: baseMaterial,
            data: {
                "item-name": `<!i><white><lang:item.animal_${itemNameKeySuffix}.${id}.name>`,
                lore: [
                    "",
                    type.includes('leather') || type === 'bone'
                        ? "<!i><white><image:atom:badge_material> <image:atom:badge_natural> <image:atom:badge_age_foraging>"
                        : "<!i><white><image:atom:badge_food> <image:atom:badge_natural> <image:atom:badge_age_foraging>",
                ],
                settings: {
                    tags: [`atom:${type}`]
                },
                "remove-components": ["attribute_modifiers"],
            },
            model: {
                template: "default:model/simplified_generated",
                arguments: {
                    path: texturePath,
                },
            },
        },
    };
}

// Translation label builder
function makeLabel(id: AnimalId, type: ItemType): string {
    const a = animalDisplay[id];
    switch (type) {
        case "raw_meat": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Raw ${b}`;
        }
        case "undercooked_meat": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Undercooked ${b}`;
        }
        case "cooked_meat": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Cooked ${b}`;
        }
        case "burnt_meat": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Burnt ${b}`;
        }
        case "leather":
            return `${a} Leather`;
        case "raw_leather":
            return `Raw ${a} Leather`;
        case "cured_leather":
            return `Cured ${a} Leather`;
        case "bone":
            return `${a} Bone`;
    }
}

function generateRecipes() {
    const recipes: Record<string, unknown> = {};

    for (const animal of animals) {
        for (const transition of recipeTransitions) {
            const fromKey = itemKey(animal, transition.from as ItemType);
            const toKey = itemKey(animal, transition.to as ItemType);
            const recipeKey = `atom:${toKey.replace('atom:', '')}_from_campfire_${transition.from}`;

            recipes[recipeKey] = {
                type: "campfire_cooking",
                category: transition.category,
                time: 2000,
                ingredient: fromKey,
                result: {
                    id: toKey,
                    count: 1,
                },
            };
        }
    }

    return recipes;
}

function generateDoc() {
    const items: Record<string, unknown> = {};
    const itemKeys: string[] = [];
    const itemTypes: ItemType[] = [
        "raw_meat",
        "undercooked_meat",
        "cooked_meat",
        "burnt_meat",
        "leather",
        "raw_leather",
        "cured_leather",
        "bone",
    ];

    // Items and list for category
    for (const id of animals) {
        for (const type of itemTypes) {
            Object.assign(items, itemBlock(id, type));
            itemKeys.push(itemKey(id, type));
        }
    }

    const categories: Record<string, unknown> = {
        [CATEGORY_KEY]: {
            ...CATEGORY,
            list: itemKeys.sort((a, b) => b.localeCompare(a)),
        },
    };

    // Translations
    const en: Record<string, string> = {
        "category.animal_product.name": "Animal Products",
        "category.animal_product.lore": "Contains all animal meats and materials",
    };
    for (const id of animals) {
        for (const t of itemTypes) {
            en[`item.animal_${t}.${id}.name`] = makeLabel(id, t);
        }
    }

    // Single document with both sections
    return {
        items,
        categories,
        lang: {
            en,
        },
    };
}

const doc = generateDoc();
const recipes = generateRecipes();

// Write recipes to a separate file
const foodRecipesPath = "../run/plugins/CraftEngine/resources/atom/configuration/food_recipes.yml";
const recipesYaml = stringify({ recipes }, { lineWidth: 0 });
writeFileSync(foodRecipesPath, recipesYaml);
console.log("Generated campfire cooking recipes into food_recipes.yml");

// Generate animal products
const yaml = stringify(doc, {lineWidth: 0});
writeFileSync("../run/plugins/CraftEngine/resources/atom/configuration/auto/animal_products.yml", yaml);
console.log("Generated animal_products.yml (items + categories + translations)");