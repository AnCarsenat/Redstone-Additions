# /ra:tools/goggles/billboard/render_literal_line
# MACRO FUNCTION - Render one literal status line.
# Input: $(x), $(y), $(z), $(scale), $(label), $(value), $(suffix), $(value_color)

$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon text_display ~$(x) ~$(y) ~$(z) {Tags:["ra.billboard","ra.display"],billboard:"center",text:[{text:"$(label)",color:"gray"},{text:"$(value)",color:"$(value_color)"},{text:"$(suffix)",color:"gray"}],background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$(scale)f,$(scale)f,$(scale)f]}}
