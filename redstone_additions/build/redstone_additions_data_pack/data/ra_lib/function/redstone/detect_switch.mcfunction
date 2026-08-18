# /ra_lib:redstone/detect_switch
# Maintain ra.powered for a block that only cares whether it is on.
# As the marker, at the block. Returns 1 or 0.
#
# For the blocks that use redstone as a switch rather than as a number — the
# Chunk Loader, the Boxer and Unboxer, both tag sensors, the Message Block, the
# wireless Emitter. They were all calling ra_lib:redstone/detect, which resolves
# the exact level on all six sides and then had its answer thrown away by a
# `matches 1..` test.
#
# This stops at the first live side instead of reading all six, so a powered
# block usually answers in a fraction of the work. An unpowered one still has to
# look everywhere before it can say no — that is the floor for asking the
# question at all, and there is no way under it that does not involve the block
# itself carrying a vanilla state.
#
# DOES NOT WRITE ra.power. Callers must test the ra.powered tag, not the score,
# or they will read whatever the last full detect left behind.

tag @s remove ra.powered
tag @s remove ra.powered.strong

execute if function ra_lib:redstone/any run tag @s add ra.powered

execute if entity @s[tag=ra.powered] run return 1
return 0
