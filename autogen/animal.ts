// generate_animal_products.ts
import {stringify} from "yaml";

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
    | "cooked_meat"
    | "burnt_meat"
    | "raw_leather"
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

// If you want culinary names, edit here (commented defaults show examples)
// const meatNameOverrides: Partial<
//   Record<AnimalId, { raw?: string; cooked?: string }>
// > = {
//   cow: { raw: "Raw Beef", cooked: "Steak" },
//   pig: { raw: "Raw Pork", cooked: "Cooked Pork" },
//   sheep: { raw: "Raw Mutton", cooked: "Cooked Mutton" },
//   chicken: { raw: "Raw Chicken", cooked: "Cooked Chicken" },
//   rabbit: { raw: "Raw Rabbit", cooked: "Cooked Rabbit" },
// };

function itemKey(id: AnimalId, type: ItemType) {
    if (type.includes("leather")) return `atom:animal_leather_${type.split("_")[0]}_${id}`;
    if (type.includes("meat")) return `atom:animal_meat_${type.split("_")[0]}_${id}`;
    return `atom:animal_${type}_${id}`;
}

function getTexturePathForType(type: ItemType) {
    switch (type) {
        case "raw_meat":
            return "minecraft:item/beef";
        case "cooked_meat":
            return "minecraft:item/cooked_beef";
        case "burnt_meat":
            return "minecraft:item/charcoal";
        case "raw_leather":
            return "minecraft:item/leather_meat";
        case "cured_leather":
            return "minecraft:item/leather_cured";
        case "bone":
            return "minecraft:item/bone";
        default:
            return "minecraft:item/redstone";
    }
}

function itemBlock(id: AnimalId, type: ItemType) {
    const baseMaterial = type.includes("leather") ? "leather" : type === "bone" ? "bone" : "beef";
    const itemNameKeySuffix = type; // e.g., raw_meat
    const texturePath = getTexturePathForType(type);

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
            // const o = meatNameOverrides[id]?.raw;
            // return o ?? `Raw ${a} Meat`;
            return `Raw ${a} Meat`;
        }
        case "cooked_meat": {
            // const o = meatNameOverrides[id]?.cooked;
            // return o ?? `Cooked ${a} Meat`;
            return `Cooked ${a} Meat`;
        }
        case "burnt_meat":
            return `Burnt ${a} Meat`;
        case "raw_leather":
            return `Raw ${a} Leather`;
        case "cured_leather":
            return `Cured ${a} Leather`;
        case "bone":
            return `${a} Bone`;
    }
}

function generateDoc() {
    const items: Record<string, unknown> = {};
    const itemKeys: string[] = [];
    const itemTypes: ItemType[] = [
        "raw_meat",
        "cooked_meat",
        "burnt_meat",
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
            list: itemKeys,
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

const yaml = stringify(generateDoc(), {lineWidth: 0});
await Bun.write("../run/plugins/CraftEngine/resources/atom/configuration/auto/animal_products.yml", yaml);
console.log("Generated animal_products.yml (items + categories + translations)");