// scripts/generators/animal_products.ts
import {
    asYamlDoc,
    atom,
    buildItemEntry,
    type AnyRecipe,
} from "@lib/itemkit";
import { OUT } from "@lib/paths";
import type { Generator } from "./types";

// ---- Domain data ----
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
    | "meat_raw"
    | "meat_undercooked"
    | "meat_cooked"
    | "meat_burnt"
    | "leather_raw"
    | "leather"
    | "leather_cured"
    | "bone"
    | "intestine"
    | "heart"
    | "liver"
    | "kidney"
    | "lungs"
    | "fat";

// Animals that have each organ type
const organsForAnimal: Record<AnimalId, ItemType[]> = {
    cow: ["intestine", "heart", "liver", "kidney", "lungs", "fat"],
    pig: ["intestine", "heart", "liver", "kidney", "lungs", "fat"],
    sheep: ["intestine", "heart", "liver", "kidney", "lungs"],
    chicken: ["intestine", "heart", "liver"],
    rabbit: ["intestine"],
    horse: ["intestine", "heart", "liver", "kidney", "lungs"],
    donkey: ["intestine", "heart", "liver", "kidney", "lungs"],
    mule: ["intestine", "heart", "liver", "kidney", "lungs"],
    llama: ["intestine", "heart", "liver", "kidney", "lungs"],
    goat: ["intestine", "heart", "liver", "kidney", "lungs"],
    cat: ["intestine"],
    wolf: ["intestine"],
    fox: ["intestine"],
    panda: ["intestine", "lungs"],
    polar_bear: ["intestine", "heart", "liver", "lungs"],
    ocelot: ["intestine"],
    camel: ["intestine", "heart", "liver", "kidney", "lungs", "fat"],
};

const CATEGORY_KEY = "atom:animal_product";
const CATEGORY = {
    name: "<!i><white><lang:category.animal_product.name></white>",
    hidden: true,
    lore: ["<!i><gray><lang:category.animal_product.lore>"],
    icon: "minecraft:leather",
    list: [] as string[],
};

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
    { from: "meat_raw", to: "meat_undercooked", category: "food" },
    { from: "meat_undercooked", to: "meat_cooked", category: "food" },
    { from: "meat_cooked", to: "meat_burnt", category: "food" },
] as const;

// ---- Helpers ----
// New shorter naming: atom:meat_raw_cow, atom:organ_cow, atom:bone_cow
function itemKey(id: AnimalId, type: ItemType) {
    return atom(`${type}_${id}`);
}

function textureForType(type: ItemType) {
    switch (type) {
        case "meat_raw":
            return "minecraft:item/meat/raw";
        case "meat_undercooked":
            return "minecraft:item/meat/undercooked";
        case "meat_cooked":
            return "minecraft:item/meat/cooked";
        case "meat_burnt":
            return "minecraft:item/meat/burnt";
        case "leather_raw":
            return "minecraft:item/leather_meat";
        case "leather":
            return "minecraft:item/leather";
        case "leather_cured":
            return "minecraft:item/leather_cured";
        case "bone":
            return "minecraft:item/meat/bone";
        case "intestine":
            return "minecraft:item/organ/intestine";
        case "heart":
            return "minecraft:item/organ/heart";
        case "liver":
            return "minecraft:item/organ/liver";
        case "kidney":
            return "minecraft:item/organ/kidney";
        case "lungs":
            return "minecraft:item/organ/lungs";
        case "fat":
            return "minecraft:item/organ/fat";
        default:
            return "minecraft:item/redstone";
    }
}

function baseMaterialForType(type: ItemType): string {
    switch (type) {
        case "leather_raw":
        case "leather":
        case "leather_cured":
            return "leather";
        case "bone":
            return "bone";
        case "intestine":
        case "heart":
        case "liver":
        case "kidney":
        case "lungs":
            return "porkchop";
        case "fat":
            return "honeycomb";
        default:
            return "beef";
    }
}

function makeLabel(id: AnimalId, type: ItemType): string {
    const a = animalDisplay[id];
    switch (type) {
        case "meat_raw": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Raw ${b}`;
        }
        case "meat_undercooked": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Undercooked ${b}`;
        }
        case "meat_cooked": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Cooked ${b}`;
        }
        case "meat_burnt": {
            const b = meatNameOverrides[id] ?? `${a} Meat`;
            return `Burnt ${b}`;
        }
        case "leather":
            return `${a} Leather`;
        case "leather_raw":
            return `Raw ${a} Leather`;
        case "leather_cured":
            return `Cured ${a} Leather`;
        case "bone":
            return `${a} Bone`;
        case "intestine":
            return `${a} Organs`;
        case "heart":
            return `${a} Heart`;
        case "liver":
            return `${a} Liver`;
        case "kidney":
            return `${a} Kidney`;
        case "lungs":
            return `${a} Lungs`;
        case "fat":
            return `${a} Fat`;
    }
}

function itemBlock(id: AnimalId, type: ItemType) {
    const baseMaterial = baseMaterialForType(type);
    const texturePath = textureForType(type);
    const name = `<!i><white><lang:item.${type}.${id}.name>`;
    const isFood = !(type.includes("leather") || type === "bone" || type === "fat");
    const loreBadge = isFood
        ? "<!i><white><image:atom:badge_food> <image:atom:badge_natural> <image:atom:badge_age_foraging>"
        : "<!i><white><image:atom:badge_material> <image:atom:badge_natural> <image:atom:badge_age_foraging>";

    return buildItemEntry(
        itemKey(id, type),
        baseMaterial,
        name,
        ["", loreBadge],
        texturePath,
        {
            removeComponents: ["attribute_modifiers"],
            tags: [`atom:${type}`],
        },
    );
}

// ---- Generators ----
function generateRecipes() {
    const recipes: Record<string, AnyRecipe> = {};
    for (const animal of animals) {
        for (const transition of recipeTransitions) {
            const fromKey = itemKey(animal, transition.from);
            const toKey = itemKey(animal, transition.to);
            const recipeKey = atom(
                `${toKey.replace("atom:", "")}_from_campfire`,
            );

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

    // Base item types that all animals have
    const baseItemTypes: ItemType[] = [
        "meat_raw",
        "meat_undercooked",
        "meat_cooked",
        "meat_burnt",
        "leather",
        "leather_raw",
        "leather_cured",
        "bone",
    ];

    for (const id of animals) {
        // Add base items
        for (const type of baseItemTypes) {
            Object.assign(items, itemBlock(id, type));
            itemKeys.push(itemKey(id, type));
        }

        // Add organ items based on what this animal has
        const organTypes = organsForAnimal[id] ?? [];
        for (const type of organTypes) {
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

    const en: Record<string, string> = {
        "category.animal_product.name": "Animal Products",
        "category.animal_product.lore":
            "Contains all animal meats and materials",
    };

    // All item types including organs
    const allItemTypes: ItemType[] = [
        ...baseItemTypes,
        "intestine",
        "heart",
        "liver",
        "kidney",
        "lungs",
        "fat",
    ];
    
    for (const id of animals) {
        for (const t of allItemTypes) {
            // Only add translation if this animal has this item type
            if (baseItemTypes.includes(t) || (organsForAnimal[id]?.includes(t))) {
                en[`item.${t}.${id}.name`] = makeLabel(id, t);
            }
        }
    }

    return { items, categories, lang: { en } };
}

export const generateAnimalProducts: Generator = () => {
    const doc = generateDoc();
    const recipes = generateRecipes();

    const docYaml = asYamlDoc(doc);
    const recipesYaml = asYamlDoc({ recipes });

    return {
        writes: [
            { path: OUT.animalProducts, content: docYaml },
            { path: OUT.foodRecipes, content: recipesYaml },
        ],
    };
};
