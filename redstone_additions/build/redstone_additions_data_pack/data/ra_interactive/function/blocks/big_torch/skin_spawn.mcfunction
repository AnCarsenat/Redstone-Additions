# /ra_interactive:blocks/big_torch/skin_spawn {block_light:N}
# Internal: summon the torch display. See skin.mcfunction for where the numbers
# come from.
#
# Centre-anchored like every other skin in the pack, so "is this one mine?" is a
# question a 0.4 radius can answer -- from a block centre a neighbour's centre is
# a full block away and its corners 0.866, and both are clear of 0.4.
#
# Lit as a torch rather than sampled: this is a light source, and a display that
# dimmed at night would be the one thing in the build that looked switched off.

$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra","ra.display","ra.skin","ra.skin.big_torch"],brightness:{sky:15,block:15},block_state:{Name:"minecraft:torch"},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-1.1f,-0.51f,-1.1f],scale:[2.2f,1.632f,2.2f]}}
