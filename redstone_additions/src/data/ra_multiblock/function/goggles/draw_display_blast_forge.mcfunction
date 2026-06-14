# /ra_multiblock:goggles/draw_display_blast_forge
# Draw goggles billboards for blast forge multiblock
# Context: as blast forge marker entity, at base position
# Shows: name, heat/enabled status, I/O labels above structure (outside the forge)

# === Name billboard (above the ceiling, y+2.5) ===
summon text_display ~ ~2.5 ~ {Tags:["ra.billboard","ra.display"],billboard:"center",text:{text:"Blast Forge",color:"green",bold:true},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}

# === Status billboards ===
function ra:tools/goggles/status/read_status_multiblock

# === I/O indicators — positioned ABOVE the structure (y+2.3 instead of y+1.3) ===
# Copy IO block offset to temp storage, add label + color, call generic show_io_label macro

# Input 1 (Material)
execute if data entity @s data.inputs.input_1 run data modify storage ra:temp io set from entity @s data.inputs.input_1
execute if data entity @s data.inputs.input_1 run data modify storage ra:temp io.label set value "Input (Material)"
execute if data entity @s data.inputs.input_1 run data modify storage ra:temp io.color set value "green"
execute if data entity @s data.inputs.input_1 run function ra_multiblock:goggles/show_io_label with storage ra:temp io

# Input 2 (Fuel)
execute if data entity @s data.inputs.input_2 run data modify storage ra:temp io set from entity @s data.inputs.input_2
execute if data entity @s data.inputs.input_2 run data modify storage ra:temp io.label set value "Input (Fuel)"
execute if data entity @s data.inputs.input_2 run data modify storage ra:temp io.color set value "green"
execute if data entity @s data.inputs.input_2 run function ra_multiblock:goggles/show_io_label with storage ra:temp io

# Output 1
execute if data entity @s data.outputs.output_1 run data modify storage ra:temp io set from entity @s data.outputs.output_1
execute if data entity @s data.outputs.output_1 run data modify storage ra:temp io.label set value "Output"
execute if data entity @s data.outputs.output_1 run data modify storage ra:temp io.color set value "red"
execute if data entity @s data.outputs.output_1 run function ra_multiblock:goggles/show_io_label with storage ra:temp io

# Control (Redstone) — outside the structure, ok at lower Y
execute if data entity @s data.controls.redstone_in run data modify storage ra:temp io set from entity @s data.controls.redstone_in
execute if data entity @s data.controls.redstone_in run data modify storage ra:temp io.label set value "Redstone Control"
execute if data entity @s data.controls.redstone_in run data modify storage ra:temp io.color set value "yellow"
execute if data entity @s data.controls.redstone_in run function ra_multiblock:goggles/show_io_label with storage ra:temp io
