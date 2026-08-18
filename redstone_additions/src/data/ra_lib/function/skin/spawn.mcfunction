# /ra_lib:skin/spawn {skin:"...",id:"...",facing:"north"}
# Internal: summon one oriented skin.
#
# Scaled a hair over 1 and pulled back by half the excess, so the skin encloses
# the real block and their surfaces never share a plane. Sitting exactly on 1.0
# would z-fight with the block underneath.
#
# A block_display has no collision and no interaction box, so the real block
# behind it still takes right-clicks, hopper insertion and comparator reads.
#
# `brightness` is set because a display samples the light at its own position,
# and its own position is inside an opaque block, where the light level is zero.
# Without this every skin renders pitch black. sky:15 / block:0 lights it as if
# it were an ordinary surface block, so it still dims at night instead of glowing.
#
# ANCHORED AT THE BLOCK CENTRE, NOT THE CORNER
# A block_display draws its block from its own position outward, so the obvious
# thing is to stand it on the block's minimum corner. That is what this used to
# do, and it made every "is this skin mine?" test ambiguous: a marker sits at the
# block CENTRE, and from a centre all eight surrounding corners are the same
# 0.866 away — our own skin and the neighbours' at +x, +y and +z included. No
# radius could separate them, so adjacent skinned blocks deleted each other's
# appearance and then each decided the other's leftover skin was its own.
#
# Standing the display at the centre instead and pushing the model back by half a
# block through the transformation puts it in exactly the same place on screen,
# while making the entity sit where the marker sits: distance 0 for ours, a clean
# 1.0 for the neighbour's. The translation is -(0.5 + 0.002): half a block to
# recentre, plus the same hair of overshoot as before to stop z-fighting.
$execute align xyz positioned ~0.5 ~0.5 ~0.5 run summon block_display ~ ~ ~ {Tags:["ra.display","ra.skin","ra.skin.$(id)"],brightness:{sky:15,block:0},block_state:{Name:"$(skin)",Properties:{facing:"$(facing)"}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.502f,-0.502f,-0.502f],scale:[1.004f,1.004f,1.004f]}}
