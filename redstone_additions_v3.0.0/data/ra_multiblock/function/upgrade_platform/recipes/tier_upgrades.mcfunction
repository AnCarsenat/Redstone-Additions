# /ra_multiblock:upgrade_platform/recipes/tier_upgrades
# Material tier upgrade recipes
# Input: storage ra:temp upgrade.input
# Output: storage ra:temp upgrade.result (if matched)

# === Ingot Upgrades ===
# Copper → Iron
execute if data storage ra:temp upgrade.input{id:"minecraft:copper_ingot"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:iron_ingot",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Iron → Gold
execute if data storage ra:temp upgrade.input{id:"minecraft:iron_ingot"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:gold_ingot",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Gold → Diamond
execute if data storage ra:temp upgrade.input{id:"minecraft:gold_ingot"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:diamond",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Diamond → Netherite Scrap
execute if data storage ra:temp upgrade.input{id:"minecraft:diamond"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:netherite_scrap",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# === Raw Ore Upgrades ===
# Raw Copper → Raw Iron
execute if data storage ra:temp upgrade.input{id:"minecraft:raw_copper"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:raw_iron",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Raw Iron → Raw Gold
execute if data storage ra:temp upgrade.input{id:"minecraft:raw_iron"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:raw_gold",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# === Gem Upgrades ===
# Coal → Lapis Lazuli
execute if data storage ra:temp upgrade.input{id:"minecraft:coal"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:lapis_lazuli",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Lapis Lazuli → Redstone
execute if data storage ra:temp upgrade.input{id:"minecraft:lapis_lazuli"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:redstone",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Redstone → Emerald
execute if data storage ra:temp upgrade.input{id:"minecraft:redstone"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:emerald",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1

# Emerald → Amethyst Shard
execute if data storage ra:temp upgrade.input{id:"minecraft:emerald"} run data modify storage ra:temp upgrade.result set value {id:"minecraft:amethyst_shard",count:1}
execute if data storage ra:temp upgrade.result run data modify storage ra:temp upgrade.chance set value 10
execute if data storage ra:temp upgrade.result run return 1
