# /ra_lib:skin/apply_lit {real:"minecraft:barrel",skin:"minecraft:furnace",id:"electric_generator",lit:"true"}
# Draw `skin` over the `real` block, carrying a `lit` block state.
# Context: at the block position.
#
# WHY THIS IS NOT JUST apply WITH A STATE IN THE NAME
# A block_display's block_state is a compound: {Name:"minecraft:furnace",
# Properties:{...}}. Name is a resource location and nothing else -- writing
# "minecraft:furnace[lit=true]" into it puts square brackets in a resource
# location, which cannot resolve, so the display spawns showing nothing at all.
# It is not an error you see in chat: the entity exists, it is simply invisible,
# and the block looks like it lost its skin. That is exactly what happened to the
# EU Generator when it was taught to glow.
#
# Only `lit` is supported, and only because two blocks in the pack want it. A
# general "any properties" version would have to build the compound as text and
# the quoting does not survive a macro argument.

$execute align xyz positioned ~0.5 ~0.5 ~0.5 run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]
$execute align xyz run kill @e[type=block_display,tag=ra.skin.$(id),distance=..0.4]

$execute unless block ~ ~ ~ $(real) run return 0

# How bright to draw it. A block_display samples light at its OWN position, which
# is inside the block it is drawing, where the light is always zero -- so without
# an override every skin renders pitch black. It used to be hardcoded to
# block:0, which is right in daylight and wrong beside a torch: the real block
# would be lit and its skin would not.
#
# Sampled one block ABOVE, because that is the light a player standing next to
# the block sees, and because the block's own space is opaque.
execute positioned ~ ~1 ~ run function ra_lib:skin/light_here
execute store result storage ra:temp skin.block_light int 1 run scoreboard players get #skin_light ra.temp

$execute if block ~ ~ ~ $(real)[facing=north] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"north",lit:"$(lit)"}
$execute if block ~ ~ ~ $(real)[facing=south] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"south",lit:"$(lit)"}
$execute if block ~ ~ ~ $(real)[facing=east] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"east",lit:"$(lit)"}
$execute if block ~ ~ ~ $(real)[facing=west] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"west",lit:"$(lit)"}
$execute if block ~ ~ ~ $(real)[facing=up] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"up",lit:"$(lit)"}
$execute if block ~ ~ ~ $(real)[facing=down] run function ra_lib:skin/spawn_lit_with_light {skin:"$(skin)",id:"$(id)",facing:"down",lit:"$(lit)"}

return 1
