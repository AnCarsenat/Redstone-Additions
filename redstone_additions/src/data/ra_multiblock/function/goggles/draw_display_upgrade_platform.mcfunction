# /ra_multiblock:goggles/draw_display_upgrade_platform
# Draw goggles billboards for upgrade platform multiblock
# Context: as upgrade platform marker entity, at base position
# Shows: name only (no IO ports, no status)

# === Name billboard (above the platform, y+1.5) ===
# Platform is 3x1x3 (1 block tall) so y+1.5 is well above
summon text_display ~ ~1.5 ~ {Tags:["ra.billboard","ra.display"],billboard:"center",text:{text:"Upgrade Platform",color:"green",bold:true},background:1073741824,shadow:true,see_through:false,line_width:200,transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[0.6f,0.6f,0.6f]}}
