# /ra_storage:blocks/unboxer/spawn_display {facing:"north"}
# Internal: lay one dispenser skin over the barrel.
#
# Scaled a hair over 1 and pulled back by half of the excess, so it fully encloses
# the barrel and the two surfaces never land on the same plane. Sitting exactly on
# 1.0 would z-fight with the block underneath.
#
# A block_display has no collision and no interaction box, so right-clicking still
# opens the barrel behind it.

$execute align xyz run summon block_display ~ ~ ~ {Tags:["ra.display","ra.custom_block.display.unboxer"],block_state:{Name:"minecraft:dispenser",Properties:{facing:"$(facing)",triggered:"false"}},transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[-0.002f,-0.002f,-0.002f],scale:[1.004f,1.004f,1.004f]}}
