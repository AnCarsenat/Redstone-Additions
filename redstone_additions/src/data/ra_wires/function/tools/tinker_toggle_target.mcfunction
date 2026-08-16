# /ra_wires:tools/tinker_toggle_target
# Goggles tinker action for an RA Wires block.
# Context: as the marker target.
#
# Pumps and drains no longer cycle a numeric medium id. A pump takes whatever it
# finds next to it, so there is nothing to configure; a drain instead switches
# between pulling fluid out of the world and putting it back.

# --- Drain: cycle mode ---
# The old state has to be latched before the first write, otherwise the second
# condition reads the value the first one just set and the mode never flips back.
execute if entity @s[tag=ra.custom_block.liquid_drain] unless data entity @s data.properties.mode run data modify entity @s data.properties.mode set value "drain"
tag @s remove ra.wires.mode_flip
execute if entity @s[tag=ra.custom_block.liquid_drain] if data entity @s data.properties{mode:"drain"} run tag @s add ra.wires.mode_flip
execute if entity @s[tag=ra.custom_block.liquid_drain] if entity @s[tag=ra.wires.mode_flip] run data modify entity @s data.properties.mode set value "place"
execute if entity @s[tag=ra.custom_block.liquid_drain] unless entity @s[tag=ra.wires.mode_flip] run data modify entity @s data.properties.mode set value "drain"
tag @s remove ra.wires.mode_flip
execute if entity @s[tag=ra.custom_block.liquid_drain] run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Drain mode: ",color:"gray"},{nbt:"data.properties.mode",entity:"@s",color:"aqua"}]
execute if entity @s[tag=ra.custom_block.liquid_drain] run return 1

# --- Everything else: toggle enabled ---
execute unless data entity @s data.properties.enabled run data modify entity @s data.properties.enabled set value 1b
execute if data entity @s data.properties{enabled:1b} run tag @s add ra.wires.was_enabled
execute if entity @s[tag=ra.wires.was_enabled] run data modify entity @s data.properties.enabled set value 0b
execute unless entity @s[tag=ra.wires.was_enabled] run data modify entity @s data.properties.enabled set value 1b
tag @s remove ra.wires.was_enabled

# A closed valve genuinely splits the network in two rather than just refusing to
# move fluid, so the two halves keep separate contents and separate media.
execute if entity @s[tag=ra.custom_block.liquid_valve] run function ra_wires:tools/valve_apply
execute if entity @s[tag=ra.custom_block.gas_valve] run function ra_wires:tools/valve_apply

execute if data entity @s data.properties{enabled:1b} run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Enabled",color:"green"}]
execute unless data entity @s data.properties{enabled:1b} run tellraw @a[tag=ra.wires.tinker_user,limit=1] [{text:"[Goggles] ",color:"gold"},{text:"Disabled",color:"red"}]

function ra_wires:common/update_model_local_and_neighbors
