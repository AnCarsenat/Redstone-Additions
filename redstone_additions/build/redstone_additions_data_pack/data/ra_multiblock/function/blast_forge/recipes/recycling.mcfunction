# /ra_multiblock:blast_forge/recipes/recycling
# Equipment recycling recipes — break down tools/armor into nuggets
# Input: storage ra:temp bf.material
# Output: storage ra:temp bf.result (if matched)
#
# Nugget yields based on crafting cost:
#   Sword=2 ingots (18 nuggets), Shovel=1 (9), Pick/Axe=3 (27), Hoe=2 (18)
#   Helmet=5 (45), Chest=8 (72), Legs=7 (63), Boots=4 (36)
#   Horse armor=6 (54)

# === Iron equipment ===
execute if data storage ra:temp bf.material{id:"minecraft:iron_sword"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:18,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_pickaxe"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:27,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_axe"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:27,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_shovel"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:9,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_hoe"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:18,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_helmet"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:45,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_chestplate"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:72,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_leggings"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:63,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_boots"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:36,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:iron_horse_armor"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:54,components:{}}
execute if data storage ra:temp bf.result run return 1

# === Gold equipment ===
execute if data storage ra:temp bf.material{id:"minecraft:golden_sword"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:18,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_pickaxe"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:27,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_axe"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:27,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_shovel"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:9,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_hoe"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:18,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_helmet"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:45,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_chestplate"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:72,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_leggings"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:63,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_boots"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:36,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:golden_horse_armor"} run data modify storage ra:temp bf.result set value {id:"minecraft:gold_nugget",count:54,components:{}}
execute if data storage ra:temp bf.result run return 1

# === Chainmail → iron nuggets ===
execute if data storage ra:temp bf.material{id:"minecraft:chainmail_helmet"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:45,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:chainmail_chestplate"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:72,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:chainmail_leggings"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:63,components:{}}
execute if data storage ra:temp bf.result run return 1

execute if data storage ra:temp bf.material{id:"minecraft:chainmail_boots"} run data modify storage ra:temp bf.result set value {id:"minecraft:iron_nugget",count:36,components:{}}
execute if data storage ra:temp bf.result run return 1

# No match
return 0
