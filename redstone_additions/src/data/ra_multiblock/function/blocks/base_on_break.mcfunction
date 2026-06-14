# /ra_multiblock:blocks/base_on_break
# MACRO FUNCTION — Handle break for any tier multiblock base
# Called: function ... with storage ra:multiblock tier_data.{tier}
# Context: as armor stand, at position

# Kill vanilla block drop
$kill @e[type=item,nbt={Item:{id:"$(block_id)"}},distance=..2,limit=1,sort=nearest]

# Disassemble any linked multiblock (use aligned position + tight distance)
execute align xyz positioned ~0.5 ~ ~0.5 as @e[tag=ra.multiblock,distance=..0.5,limit=1,sort=nearest] run function ra_lib_multiblock:disassemble

# Drop the custom item
$summon item ~ ~ ~ {Item:{id:"minecraft:bat_spawn_egg",count:1,components:{"minecraft:item_model":"$(block_model)","minecraft:item_name":"$(tier_name) Multiblock Base","minecraft:custom_data":{ra:{multiblock_base:"$(tier)"}},"minecraft:entity_data":{id:"minecraft:bat",Tags:["ra.spawned","ra.place.multiblock_base","ra.place.multiblock_base.$(tier)"],Silent:1b,NoAI:1b,Invulnerable:1b},"minecraft:lore":[{text:"$(lore_text)",color:"gray",italic:false}]}}}

$playsound $(break_sound) block @a[distance=..16] ~ ~ ~ $(break_volume) $(break_pitch)
particle minecraft:cloud ~ ~ ~ 0.2 0.2 0.2 0.02 5

kill @s
