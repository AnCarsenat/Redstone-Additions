# /ra_wires:blocks/electric_generator/refresh_display
# Rebuild the EU Generator's furnace skin, lit or unlit.
# Context: as the generator marker, at its block position.
#
# The block underneath is a barrel, because the generator only wants somewhere to
# put fuel. A real furnace brings two input slots and its own smelting, neither of
# which mean anything here, and players fill the top slot with ore and wait. See
# ra_lib:skin/apply for the full reasoning.
#
# The skin carries the `lit` state, so a running generator glows the way a burning
# furnace does. It goes through apply_lit rather than apply: a block state cannot
# ride along in the block's NAME, and putting it there is what made this skin
# disappear entirely rather than fail loudly.
#
# Changing lit on the display that is already there beats rebuilding it -- see
# ra_lib:skin/set_lit for why a kill-and-respawn blinks and z-fights. apply_lit
# is the fallback for when there is no display to edit yet.
#
# LIT COMES FROM THE gen_lit TAG, NOT FROM data.data.burn
# It used to read data.data.burn, which is the SOLID FUEL countdown. A generator
# running on steam never sets that field -- generator_tick raises #gen.burning
# from the steam path directly -- so a steam generator produced EU while drawn
# permanently unlit. `lit` is a question about whether the block is generating,
# and data.data.burn only answers "is it burning an item", which is a different
# question that happens to coincide for one of the two fuels.
#
# running/idle already maintain gen_lit for BOTH fuel paths, so that tag is the
# honest answer. idle must therefore clear gen_lit BEFORE calling this.

data modify storage ra:wires gen.skin.id set value "electric_generator"
data modify storage ra:wires gen.skin.lit set value "false"
execute if entity @s[tag=ra.wires.gen_lit] run data modify storage ra:wires gen.skin.lit set value "true"

execute store result score #gen.skinned ra.wires.tmp run function ra_lib:skin/set_lit with storage ra:wires gen.skin
execute if score #gen.skinned ra.wires.tmp matches 1.. run return 0

execute if entity @s[tag=ra.wires.gen_lit] run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"true"}
execute unless entity @s[tag=ra.wires.gen_lit] run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"false"}
