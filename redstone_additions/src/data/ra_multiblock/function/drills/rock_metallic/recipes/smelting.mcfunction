# /ra_multiblock:blast_forge/recipes/smelting
# Ore smelting recipes — doubles yield vs vanilla furnace
# Input: storage ra:temp bf.material
# Output: storage ra:temp bf.result (if matched)

# --- Raw ores → 2× ingots ---
execute if data storage ra:temp bf.material{id:"minecraft:raw_iron"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:raw_gold"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:raw_copper"} run data modify storage ra:temp bf.result set value {id:"minecraft:copper_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

# --- Ore blocks → 2× ingots ---
execute if data storage ra:temp bf.material{id:"minecraft:iron_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:deepslate_iron_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:gold_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:deepslate_gold_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:copper_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:copper_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:deepslate_copper_ore"} run data modify storage ra:temp bf.result set value {id:"minecraft:copper_ingot",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

# --- Ancient debris → 2× netherite scrap ---
execute if data storage ra:temp bf.material{id:"minecraft:ancient_debris"} run data modify storage ra:temp bf.result set value {id:"minecraft:netherite_scrap",count:2,components:{}}
execute if data storage ra:temp bf.result run return 1

# --- Sand → glass ---
execute if data storage ra:temp bf.material{id:"minecraft:sand"} run data modify storage ra:temp bf.result set value {id:"minecraft:glass",count:1,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:red_sand"} run data modify storage ra:temp bf.result set value {id:"minecraft:glass",count:1,components:{}}
execute if data storage ra:temp bf.result run return 1

# --- Cobblestone → stone ---
execute if data storage ra:temp bf.material{id:"minecraft:cobblestone"} run data modify storage ra:temp bf.result set value {id:"minecraft:stone",count:1,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:cobbled_deepslate"} run data modify storage ra:temp bf.result set value {id:"minecraft:deepslate",count:1,components:{}}
execute if data storage ra:temp bf.result run return 1

# No match
return 0
