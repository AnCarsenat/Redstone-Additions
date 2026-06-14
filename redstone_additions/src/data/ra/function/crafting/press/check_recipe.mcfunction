# /ra_lib:crafting/press/check_recipe
# Check a press crafting recipe
# Call with macros: item1_data, item2_data, result_id, result_components, craft_tag
#
# Example usage:
# function ra_lib:crafting/press/check_recipe {item1_data:{reactor:1b},item2_data:{lava_core:1b},result_id:"minecraft:bat_spawn_egg",result_components:{item_name:"Lava Reactor",custom_data:{lava_reactor:1b}},craft_tag:"ra.lava_reactor_craft"}

$execute as @e[type=item,nbt={Item:{components:{"minecraft:custom_data":$(item1_data)}}}] at @s if block ~ ~ ~ piston_head if block ~ ~-1 ~ smithing_table if entity @e[type=item,nbt={Item:{components:{"minecraft:custom_data":$(item2_data)}}},distance=..1.5] run tag @e[type=item,limit=2,sort=nearest,distance=..1] add $(craft_tag)

$execute at @e[sort=random,limit=1,tag=$(craft_tag)] run summon item ~ ~ ~ {Item:{id:"$(result_id)",count:1,components:$(result_components)}}
$execute at @e[sort=random,limit=1,tag=$(craft_tag)] run playsound minecraft:block.anvil.land block @a[distance=..40] ~ ~ ~ 0.5 1
$kill @e[tag=$(craft_tag)]
