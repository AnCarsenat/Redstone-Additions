# /ra:tools/goggles/billboard/init_offsets
# Initialize configurable text-display offsets once.

execute if data storage ra:display offsets run return 0

data modify storage ra:display offsets set value {billboard_name:{x:0.0,y:1.0,z:0.0,scale:0.5},status_line:{x:0.0,y:0.8,z:0.0,scale:0.35},io_label:{x:0.0,y:1.9,z:0.0,scale:0.4},multiblock_name:{x:0.0,y:2.1,z:0.0,scale:0.6},multiblock_io:{x:0.0,y:1.9,z:0.0,scale:0.4}}
