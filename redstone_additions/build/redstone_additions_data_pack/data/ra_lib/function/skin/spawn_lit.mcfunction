# /ra_lib:skin/spawn_lit {skin:"...",id:"...",facing:"north",lit:"true"}
# Internal: summon one oriented skin that also carries a `lit` state.
# See ra_lib:skin/spawn for the geometry, and ra_lib:skin/apply_lit for why the
# state cannot simply be written into the block name.
#
# `lit` is a string here because block state values in a block_state compound are
# always strings -- lit:true would be a boolean and would not match.

$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.display","ra.skin","ra.skin.$(id)"],brightness:{sky:15,block:$(block_light)},block_state:{Name:"$(skin)",Properties:{facing:"$(facing)",lit:"$(lit)"}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.51f,-0.51f,-0.51f],scale:[1.02f,1.02f,1.02f]}}
