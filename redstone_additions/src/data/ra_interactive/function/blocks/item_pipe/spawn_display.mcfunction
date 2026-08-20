# /ra_interactive:blocks/item_pipe/spawn_display {id:"minecraft:iron_ingot",count:1}
# Internal: summon the filter display. Small, above the pipe, no rotation -- it
# reads as a label rather than as a dropped item.

$summon item_display ~ ~0.9 ~ {item:{id:"$(id)",count:1},item_display:"fixed",billboard:"vertical",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.35f,0.35f,0.35f]},Tags:["ra","ra.display","ra.pipe_filter"]}
