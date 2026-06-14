# /ra:tools/goggles/billboard/handle_multiblock
# MACRO FUNCTION — Show multiblock name + I/O indicators
# Context: as multiblock marker entity, at base position
# Input: $(name) = display name of the multiblock

# Summon name billboard above the multiblock base
$summon text_display ~ ~2.2 ~ {Tags:["ra.billboard","ra.display"],billboard:"center",text:{text:"$(name)",color:"green",bold:true},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}

# Show I/O indicators — copy offsets + label to temp storage, then call show functions
# Green = Input, Red = Output, Yellow = Control

# Input indicators
execute if data entity @s data.inputs.input_1 run data modify storage ra:temp io set from entity @s data.inputs.input_1
execute if data entity @s data.inputs.input_1 run data modify storage ra:temp io.label set value "Input (Material)"
execute if data entity @s data.inputs.input_1 run function ra:tools/goggles/billboard/show_input with storage ra:temp io

execute if data entity @s data.inputs.input_2 run data modify storage ra:temp io set from entity @s data.inputs.input_2
execute if data entity @s data.inputs.input_2 run data modify storage ra:temp io.label set value "Input (Fuel)"
execute if data entity @s data.inputs.input_2 run function ra:tools/goggles/billboard/show_input with storage ra:temp io

# Output indicators
execute if data entity @s data.outputs.output_1 run data modify storage ra:temp io set from entity @s data.outputs.output_1
execute if data entity @s data.outputs.output_1 run data modify storage ra:temp io.label set value "Output"
execute if data entity @s data.outputs.output_1 run function ra:tools/goggles/billboard/show_output with storage ra:temp io

# Control indicators
execute if data entity @s data.controls.redstone_in run data modify storage ra:temp io set from entity @s data.controls.redstone_in
execute if data entity @s data.controls.redstone_in run data modify storage ra:temp io.label set value "Redstone Control"
execute if data entity @s data.controls.redstone_in run function ra:tools/goggles/billboard/show_control with storage ra:temp io

# Show heat status for blast forge
execute if entity @s[tag=ra.multiblock.blast_forge] run function ra:tools/goggles/status/read_status_multiblock
