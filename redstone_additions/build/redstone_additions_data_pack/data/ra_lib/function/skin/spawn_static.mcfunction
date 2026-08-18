# /ra_lib:skin/spawn_static {skin:"...",id:"..."}
# Internal: summon one unoriented skin.
#
# See ra_lib:skin/spawn for why `brightness` is set: a display inside an opaque
# block reads that block's light level, which is zero, and renders black.

# Centre-anchored with a half-block translation; see ra_lib:skin/spawn for why.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.display","ra.skin","ra.skin.$(id)"],brightness:{sky:15,block:0},block_state:{Name:"$(skin)"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.502f,-0.502f,-0.502f],scale:[1.004f,1.004f,1.004f]}}
