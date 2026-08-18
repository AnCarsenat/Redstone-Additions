# /ra_storage:blocks/unboxer/tick
# Tick all unboxer blocks

# Break detection
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s unless block ~ ~ ~ minecraft:barrel run tag @s add ra.broken
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run kill @e[type=item,nbt={Item:{id:"minecraft:barrel"}},distance=..2,limit=1,sort=nearest]
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"minecraft:dispenser","minecraft:item_name":"Unboxer","minecraft:lore":[{text:"Put crates inside; unboxes them into the block in front",color:"gray",italic:false}],"minecraft:custom_data":{ra:{unboxer:1b}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.unboxer"],Silent:1b,NoAI:1b,Invulnerable:1b}}}}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run playsound minecraft:block.stone.break block @a[distance=..16] ~ ~ ~ 1 1
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run function ra_lib:skin/clear {id:"unboxer"}
execute as @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] at @s run kill @s
tag @e[type=marker,tag=ra.broken,tag=ra.custom_block.unboxer] remove ra.broken

# Ensure IO defaults exist for macro processing
# Matches handle_placement: the Unboxer reads the crates in its own inventory.
execute as @e[type=marker,tag=ra.custom_block.unboxer] unless data entity @s data.properties.input1 run data modify entity @s data.properties.input1 set value "~ ~ ~"
execute as @e[type=marker,tag=ra.custom_block.unboxer] unless data entity @s data.properties.output1 run data modify entity @s data.properties.output1 set value "^ ^ ^1"

# Migrate legacy up/down defaults to rear/front defaults.
execute as @e[type=marker,tag=ra.custom_block.unboxer,tag=!ra.storage.io_default_migrated] if data entity @s data.properties{input1:"^ ^-1 ^",output1:"^ ^1 ^"} run data merge entity @s {data:{properties:{input1:"^ ^ ^-1",output1:"^ ^ ^1"}}}
tag @e[type=marker,tag=ra.custom_block.unboxer,tag=!ra.storage.io_default_migrated] add ra.storage.io_default_migrated

# Rebuild a skin that went missing (chunk reload, /kill, a rotation). Only the
# Unboxers actually lacking one do any work.
# Anchored to the block centre, matching where ra_lib:skin/spawn stands its
# displays. The old wide radius could not tell our skin from the neighbour's, so
# an Unboxer whose skin was missing saw the Unboxer next door's and decided it had
# nothing to repair — a pair of adjacent Unboxers could settle with one skin
# between them and never fix it. This also drives the migration off the old
# corner anchor: a pre-centre skin is 0.866 away, fails this test, and the refresh
# it triggers replaces it.
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.unboxer,distance=..0.4,limit=1] run function ra_storage:blocks/unboxer/refresh_display

# Processing
execute as @e[type=marker,tag=ra.custom_block.unboxer] at @s run function ra_lib:redstone/detect_switch
# Powered to run, as originally designed. The redstone lock introduced earlier
# only existed to avoid arming the vanilla dispenser trigger; a barrel has no
# such trigger, so there is no reason to invert the control.
execute as @e[type=marker,tag=ra.custom_block.unboxer,tag=ra.powered] at @s run function ra_storage:blocks/unboxer/process with entity @s data.properties
