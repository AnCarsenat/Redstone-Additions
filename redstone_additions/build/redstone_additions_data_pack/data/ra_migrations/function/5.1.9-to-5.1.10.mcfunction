# /ra_migrations:5.1.9-to-5.1.10
# Drop the dead `enabled` property from RA Wires blocks.
#
# 5.1.10 removed it from the module. It was a second off switch sitting next to
# redstone on blocks that mostly had nothing else to configure, and it turned the
# wrench menu into a list of one useless row. Existing markers still carry the
# field, where it would show up in the Data Handler as a property that no longer
# does anything -- worse than not being there.
#
# The EU Switch is the one that actually used it and now runs on redstone, so its
# copy has to go too or a switch saved as disabled would look configurable and be
# inert.
#
# Idempotent: removing an absent path does nothing.

execute as @e[type=marker,tag=ra.custom_block.battery] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.boiler] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.creative_eu] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.creative_fluid] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_breaker] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_furnace] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_generator] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_switch] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.electric_wire] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.gas_pump] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.gas_tank] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.gas_valve] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.industrial_light] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.liquid_valve] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.solar_panel] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.magic_crate] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.ender_item_vault] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.ender_fluid_vault] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.ender_power_vault] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.teleport_anchor] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.mineral_generator] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.nether_generator] run data remove entity @s data.properties.enabled
execute as @e[type=marker,tag=ra.custom_block.poppy_generator] run data remove entity @s data.properties.enabled
