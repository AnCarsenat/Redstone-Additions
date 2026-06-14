# /ra:tools/goggles/billboard/render_name
# MACRO FUNCTION - Render a centered block name billboard.
# Input: $(name), $(x), $(y), $(z), $(scale)

$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon text_display ~$(x) ~$(y) ~$(z) {Tags:["ra.billboard","ra.display"],billboard:"center",text:{text:"$(name)",color:"gold",bold:false},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$(scale)f,$(scale)f,$(scale)f]}}
