# /ra_storage:storage_box/spawn_with_cached_input
# Context: at front output position of boxer

summon item ~ ~ ~ {Item:{id:"minecraft:player_head",count:1,components:{"minecraft:item_name":"Storage Box","minecraft:profile":"BoxMan01234","minecraft:max_stack_size":64,"minecraft:lore":[{text:"(empty)",color:"gray",italic:false}],"minecraft:custom_data":{ra:{item_box:1b,storage_box:{items:[],preview:[]}}}}},PickupDelay:10,Tags:["ra.storage_box.new"]}

execute as @e[type=item,tag=ra.storage_box.new,distance=..0.6,limit=1,sort=nearest] run function ra_storage:storage_box/add_cached_input
tag @e[type=item,tag=ra.storage_box.new,distance=..0.6,limit=1,sort=nearest] remove ra.storage_box.new

return run scoreboard players get #boxer_added ra.temp
