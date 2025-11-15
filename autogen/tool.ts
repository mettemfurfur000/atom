// generate_items.ts
import {stringify} from "yaml";

// ---------------------- Config ----------------------


const MATERIALS = ["stone", "iron", "copper", "bronze", "steel"] as const;
type Material = (typeof MATERIALS)[number];

const ALL_TOOLS = [
    "pickaxe",
    "shovel",
    "hoe",
    "sword",
    "axe",
    "hammer",
    "knife",
    "saw",
] as const;
type ToolType = (typeof ALL_TOOLS)[number];

const CLASSIC_MATERIALS: Material[] = ["iron", "stone"];
const CLASSIC_TOOLS: ToolType[] = ["pickaxe", "shovel", "hoe", "sword", "axe"];

// Which material belongs to which tech-age badge
// Update the image keys to match your resource names.
const ageByMaterial: Record<Material, string> = {
    stone: "foraging",
    copper: "copper",
    bronze: "bronze",
    iron: "iron",
    steel: "iron",
};

// Molds
type MoldDef = {
    id: string;
    display: string;
};
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
type Mold = (typeof MOLDS)[number];
type MoldId = (typeof MOLDS)[number]["id"];
type MoldVariant = "clay" | "fired" | "wax";

// Textures
const unfiredBaseTexture = "minecraft:item/ceramic/unfired_";
const firedBaseTexture = "minecraft:item/ceramic/fired/";
const waxBaseTexture = "minecraft:item/ceramic/wax_";

function fullToolTexture(material: Material, type: ToolType): string {
    return `minecraft:item/tool/${material}_${type}`;
}

function headTexture(material: Material, type: ToolType): string {
    return `minecraft:item/tool/head/${material}_${type}`;
}

function moldTexture(def: Mold, variant: MoldVariant): string {
    switch (variant) {
        case "wax":
            return waxBaseTexture + def.id;
        case "fired":
            return firedBaseTexture + def.id + "_empty";
        case "clay":
            return unfiredBaseTexture + def.id;
    }
}

// ---------------------- Helpers ----------------------

function shouldGenerateFullTool(material: Material, type: ToolType): boolean {
    if (CLASSIC_MATERIALS.includes(material)) return !CLASSIC_TOOLS.includes(type);
    return true;
}

function toolKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}`;
}

function headKey(material: Material, type: ToolType) {
    return `atom:${material}_${type}_head`;
}

function moldKey(id: MoldId, variant: MoldVariant) {
    return `atom:${variant}_mold_${id}`;
}

function toolBaseMaterial(type: ToolType): string {
    switch (type) {
        case "pickaxe":
            return "iron_pickaxe";
        case "shovel":
            return "iron_shovel";
        case "hoe":
            return "iron_hoe";
        case "sword":
            return "iron_sword";
        case "axe":
            return "iron_axe";
        case "hammer":
            return "iron_pickaxe";
        case "knife":
            return "iron_sword";
        case "saw":
            return "iron_axe";
    }
}

const HEAD_BASE_MATERIAL = "paper";
const CLAY_BASE_MATERIAL = "clay_ball";
const FIRED_OR_WAX_BASE_MATERIAL = "brick";

// L10n builders
function l10nTool(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool.${material}.${type}.name>`;
}

function l10nToolHead(material: Material, type: ToolType) {
    return `<!i><white><lang:item.tool_head.${material}.${type}.name>`;
}

function l10nMold(variant: MoldVariant, id: MoldId) {
    return `<!i><white><lang:item.mold.${variant}.${id}.name>`;
}

// Lore helpers
const img = (key: string) => `<image:atom:${key}>`;
const badge = (key: string) => img(`badge_${key}`);
const ageBadge = (age: string) => img(`badge_age_${age}`);
const badgeAgeForMaterial = (m: Material) => ageBadge(ageByMaterial[m]);

const BADGE_MATERIAL = img("badge_material");
const BADGE_TOOL = img("badge_tool");
// molds use either material or utility badge depending on the variant in original code
const BADGE_UTILITY = img("badge_utility");

// Shared model block
function simplifiedGeneratedModel(path: string) {
    return {
        template: "default:model/simplified_generated",
        arguments: {path},
    };
}

// Small helpers to unify and deduplicate item/category construction
function buildItemEntry(
    key: string,
    baseMaterial: string,
    displayName: string,
    lore: string[],
    model: string | Record<string, any>,
    removeComponents?: string[]
) {
    return {
        [key]: {
            material: baseMaterial,
            data: {
                "item-name": displayName,
                lore,
                ...(removeComponents ? {"remove-components": removeComponents} : {}),
            },
            model: typeof model === "string" ? simplifiedGeneratedModel(model) : model,
        },
    };
}

function buildCategory(
    key: string,
    icon: string,
    list: string[],
    hidden: boolean = true
) {
    return {
        [`atom:${key}`]: {
            name: `<!i><white><lang:category.${key}.name></white>`,
            hidden,
            lore: [`<!i><gray><lang:category.${key}.lore>`],
            icon,
            list,
        },
    } as const;
}

// ---------------------- Item builders ----------------------

function buildToolHead(material: Material, type: ToolType) {
    const key = headKey(material, type);
    const lore = [
        "<!i><gray><lang:item.tool_head.common.lore>",
        "",
        `<!i><white>${BADGE_MATERIAL} ${badgeAgeForMaterial(material)}`,
    ];
    return buildItemEntry(
        key,
        HEAD_BASE_MATERIAL,
        l10nToolHead(material, type),
        lore,
        headTexture(material, type)
    );
}

function buildFullTool(material: Material, type: ToolType) {
    const key = toolKey(material, type);
    const lore = [
        "<!i><gray><lang:item.tool.common.lore>",
        "",
        `<!i><white>${BADGE_TOOL} ${badgeAgeForMaterial(material)}`,
    ];
    return buildItemEntry(
        key,
        toolBaseMaterial(type),
        l10nTool(material, type),
        lore,
        fullToolTexture(material, type)
    );
}

function buildMold(def: Mold, variant: MoldVariant) {
    const key = moldKey(def.id, variant);
    const baseMaterial =
        variant === "clay" ? CLAY_BASE_MATERIAL : FIRED_OR_WAX_BASE_MATERIAL;

    const lore: (string | null)[] = [
        "<!i><gray><lang:item.mold.common.lore>",
        variant === "clay" ? "<!i><red><lang:item.mold.unfired.lore>" : null,
        "",
        `<!i><dark_gray><lang:item.mold.${def.id}.lore>`,
        "",
        // Keep original badges: clay uses material+age; fired/wax use utility+age
        variant === "clay"
            ? `<!i><white>${badge("material")} ${ageBadge("copper")}`
            : `<!i><white>${badge("utility")} ${ageBadge("copper")}`,
    ];

    return buildItemEntry(
        key,
        baseMaterial,
        l10nMold(variant, def.id),
        lore.filter((l): l is string => l !== null),
        moldTexture(def, variant)
    );
}

// Filled molds (fired/wax) helpers/builders
function filledMoldKey(id: MoldId, variant: "fired" | "wax") {
    return `atom:filled_${variant}_mold_${id}`;
}

function filledMoldTextures(def: Mold, variant: "fired" | "wax") {
    const layer0 = variant === "fired"
        ? `${firedBaseTexture}${def.id}_empty`
        : `${waxBaseTexture}${def.id}`;
    const layer1 = `${firedBaseTexture}${def.id}_overlay`
    return {layer0, layer1};
}

function filledMoldModel(def: Mold, variant: "fired" | "wax") {
    const tex = filledMoldTextures(def, variant);
    return {
        type: "minecraft:model",
        path: `minecraft:item/custom/gen/tool/filled_${variant}_${def.id}`,
        generation: {
            parent: "minecraft:item/handheld",
            textures: {
                "layer0": tex.layer0,
                "layer1": tex.layer1,
            },
        },
        tints: [
            {type: "minecraft:constant", value: "255,255,255"},
            {type: "minecraft:dye", default: "255,255,255"},
        ],
    };
}

function buildFilledMold(def: Mold, variant: "fired" | "wax") {
    const key = filledMoldKey(def.id, variant);

    const lore: (string | null)[] = [
        "<!i><gray>Filled with: <gold>MATERIAL_HERE</gold>",
        "",
        `<!i><dark_gray><lang:item.mold.${def.id}.lore>`,
        "",
        `<!i><white>${badge("utility")} ${ageBadge("copper")}`,
    ];

    return buildItemEntry(
        key,
        FIRED_OR_WAX_BASE_MATERIAL,
        l10nMold(variant, def.id),
        lore.filter((l): l is string => l !== null),
        filledMoldModel(def, variant)
    );
}

function buildToolRecipe(tool: ToolType, material: Material) {
    let result = `atom:${material}_${tool}`
    if(!shouldGenerateFullTool(material, tool)) {
        result = `minecraft:${material}_${tool}`
    }
    if(material == "stone") {
        return {
            type: "shaped",
            pattern: ["H", "V", "S"],
            ingredients: {H: headKey(material, tool), V: "minecraft:vine", S: "minecraft:stick"},
            result: {id: result, count: 1},
        }
    } else {
        return {
            type: "shaped",
            pattern: ["H", "S"],
            ingredients: {H: headKey(material, tool), S: "minecraft:stick"},
            result: {id: result, count: 1},
        }
    }
}

// ---------------------- Generators ----------------------

function generateMolds() {
    const items: Record<string, unknown> = {};
    const list: string[] = [];
    const variants: MoldVariant[] = ["clay", "fired", "wax"];

    // Base molds (clay/fired/wax)
    for (const def of MOLDS) {
        for (const v of variants) {
            Object.assign(items, buildMold(def, v));
            list.push(moldKey(def.id, v));
        }
    }

    // Filled molds (fired/wax) — like items/test.yml
    const filledVariants = ["fired", "wax"] as const;
    for (const def of MOLDS) {
        for (const v of filledVariants) {
            Object.assign(items, buildFilledMold(def, v));
            list.push(filledMoldKey(def.id, v));
        }
    }

    const categories = {
        ...buildCategory("molds", "atom:clay_mold_shovel_head", list),
    };

    const en: Record<string, string> = {
        "category.molds.name": "Clay and Fired Molds",
        "category.molds.lore": "Reusable molds for shaping tools and parts",
        "item.mold.common.lore": "A mold used in crafting tools and weapons",
        "item.mold.unfired.lore": "Fire this in a kiln to create a mold",
    };

    for (const def of MOLDS) {
        en[`item.mold.wax.${def.id}.name`] = `Wax ${def.display} Mold`;
        en[`item.mold.clay.${def.id}.name`] = `Clay ${def.display} Mold`;
        en[`item.mold.fired.${def.id}.name`] = `Fired ${def.display} Mold`;
        const connecting =
            def.display.toLowerCase().charAt(0) === "a" ? "an" : "a";
        en[`item.mold.${def.id}.lore`] = `Used to make ${connecting} ${def.display}`;
    }

    return {items, categories, lang: {en}};
}

function generateTools() {
    const items: Record<string, unknown> = {};
    const recipes: Record<string, unknown> = {};
    const headKeys: string[] = [];
    const toolKeys: string[] = [];

    for (const mat of MATERIALS) {
        for (const t of ALL_TOOLS) {
            Object.assign(items, buildToolHead(mat, t));
            headKeys.push(headKey(mat, t));

            if (shouldGenerateFullTool(mat, t)) {
                Object.assign(items, buildFullTool(mat, t));
                recipes[toolKey(mat, t)] = buildToolRecipe(t, mat);
                toolKeys.push(toolKey(mat, t));
            }
        }
    }

    const categories = {
        ...buildCategory("tools", "atom:copper_pickaxe", toolKeys.sort((a, b) => a.localeCompare(b))),
        ...buildCategory("tool_heads", "atom:copper_pickaxe_head", headKeys.sort((a, b) => a.localeCompare(b))),
    };

    const en: Record<string, string> = {
        "category.tools.name": "Tools",
        "category.tools.lore": "Crafted tools by material",
        "category.tool_heads.name": "Tool Heads",
        "category.tool_heads.lore": "Components used to craft tools",
        "item.tool_head.common.lore": "A shaped head for a tool",
        "item.tool.common.lore": "A durable tool for survival tasks",
    };

    const materialName: Record<Material, string> = {
        stone: "Stone",
        copper: "Copper",
        bronze: "Bronze",
        iron: "Iron",
        steel: "Steel",
    };
    const typeName: Record<ToolType, string> = {
        pickaxe: "Pickaxe",
        shovel: "Shovel",
        hoe: "Hoe",
        sword: "Sword",
        axe: "Axe",
        hammer: "Hammer",
        knife: "Knife",
        saw: "Saw",
    };

    const headSuffix: Record<ToolType, string> = {
        pickaxe: "Head",
        shovel: "Head",
        hoe: "Head",
        sword: "Blade",
        axe: "Head",
        hammer: "Head",
        knife: "Blade",
        saw: "Blade",
    };

    for (const m of MATERIALS) {
        for (const t of ALL_TOOLS) {
            en[`item.tool_head.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]} ${headSuffix[t]}`;
            en[`item.tool.${m}.${t}.name`] = `${materialName[m]} ${typeName[t]}`;
        }
    }

    return {items, categories, lang: {en}, recipes};
}

// ---------------------- Orchestrate + Write ----------------------

function deepMerge<T extends Record<string, any>>(
    a: T,
    b: T
): T {
    const out: any = Array.isArray(a) ? [...a] : {...a};
    for (const [k, v] of Object.entries(b)) {
        if (v && typeof v === "object" && !Array.isArray(v)) {
            out[k] = deepMerge(out[k] ?? {}, v);
        } else {
            out[k] = v;
        }
    }
    return out;
}

function asYamlDoc(doc: any) {
    return stringify(doc, {lineWidth: 0});
}

async function main() {
    const molds = generateMolds();
    const tools = generateTools();

    // Keep separate YAML files (same paths you used). You can also merge to one.
    const moldsYaml = asYamlDoc(molds);
    const toolsYaml = asYamlDoc(tools);

    await Bun.write(
        "../run/plugins/CraftEngine/resources/atom/configuration/auto/molds.yml",
        moldsYaml
    );
    await Bun.write(
        "../run/plugins/CraftEngine/resources/atom/configuration/auto/tools.yml",
        toolsYaml
    );

    console.log(
        "Generated molds.yml (items + category + translations) and tools.yml (heads + tools)"
    );
}

await main();