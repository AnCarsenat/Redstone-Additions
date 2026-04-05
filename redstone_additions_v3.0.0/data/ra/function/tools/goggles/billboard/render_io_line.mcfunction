# /ra:tools/goggles/billboard/render_io_line
# MACRO FUNCTION - Render one I/O label.
# Input: $(x), $(y), $(z), $(ox), $(oy), $(oz), $(label), $(color), $(scale)

$execute positioned ~$(x) ~$(y) ~$(z) align xyz positioned ~0.5 ~0.5 ~0.5 run summon text_display ~$(ox) ~$(oy) ~$(oz) {Tags:["ra.billboard","ra.display"],billboard:"center",text:{text:"$(label)",color:"$(color)",italic:false},background:553648127,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$(scale)f,$(scale)f,$(scale)f]}}
