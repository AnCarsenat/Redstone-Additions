# /ra_wires:blocks/electric_generator/refresh_display
# Rebuild the EU Generator's furnace skin.
# Context: at the generator's block position.
#
# The block underneath is a barrel, because the generator only wants somewhere to
# put fuel. A real furnace brings its own two input slots and its own smelting,
# neither of which mean anything here, and players fill the top slot with ore and
# wait. See ra_lib:skin/apply for the full reasoning.

function ra_lib:skin/apply {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator"}
