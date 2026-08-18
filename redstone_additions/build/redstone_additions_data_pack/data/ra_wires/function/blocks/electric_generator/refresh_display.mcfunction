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

execute if data entity @s data.data.burn run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"true"}
execute unless data entity @s data.data.burn run function ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"false"}
