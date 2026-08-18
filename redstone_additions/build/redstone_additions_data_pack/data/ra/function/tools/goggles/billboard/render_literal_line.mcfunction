# /ra:tools/goggles/billboard/render_literal_line
# MACRO FUNCTION - Render one literal status line.
# Input: $(x), $(y), $(z), $(scale), $(label), $(value), $(suffix), $(value_color)
#
# THE ANCHOR IS ABOVE THE BLOCK, NOT INSIDE IT
# This used to anchor at the block centre (~0.5 ~0.5 ~0.5), which put the top
# face of the block at y+0.5. Every ladder in the pack counts downwards from
# its first line, so the third line onwards landed under that top face and the
# text rendered inside the block that owned it — readable only by standing in
# the wall. Anchoring at ~1.3 lifts the whole ladder clear: the lowest offset
# any caller uses is -0.2, which now sits at y+1.1, just above the block.
#
# render_name uses the same anchor, so the name still sits above the lines
# rather than the two swapping places.

$execute align xyz positioned ~0.5 ~1.3 ~0.5 run summon text_display ~$(x) ~$(y) ~$(z) {Tags:["ra","ra.billboard","ra.display"],billboard:"center",text:[{text:"$(label)",color:"gray"},{text:"$(value)",color:"$(value_color)"},{text:"$(suffix)",color:"gray"}],background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[$(scale)f,$(scale)f,$(scale)f]}}
