# /ra_lib:skin/spawn {skin:"...",id:"...",facing:"north"}
# Internal: summon one oriented skin.
#
# Scaled a hair over 1 and pulled back by half the excess, so the skin encloses
# the real block and their surfaces never share a plane. Sitting exactly on 1.0
# would z-fight with the block underneath.
#
# A block_display has no collision and no interaction box, so the real block
# behind it still takes right-clicks, hopper insertion and comparator reads.

$execute align xyz run summon block_display ~ ~ ~ {Tags:["ra.display","ra.skin","ra.skin.$(id)"],block_state:{Name:"$(skin)",Properties:{facing:"$(facing)"}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.002f,-0.002f,-0.002f],scale:[1.004f,1.004f,1.004f]}}
