# /ra:tools/goggles/draw_block
# Dispatch to whichever block this marker is.
# Context: as the block's marker, at the block position.
#
# Pure routing. What each block shows, and what it is called, are defined by
# that block in blocks/<name>/goggles.mcfunction. ra:tools/block_name reuses
# this same dispatch to resolve just the name.

# ra_interactive
execute if entity @s[tag=ra.custom_block.block_breaker] run function ra_interactive:blocks/block_breaker/goggles
execute if entity @s[tag=ra.custom_block.block_placer] run function ra_interactive:blocks/block_placer/goggles
execute if entity @s[tag=ra.custom_block.breeder] run function ra_interactive:blocks/breeder/goggles
execute if entity @s[tag=ra.custom_block.infinite_water_cauldron] run function ra_interactive:blocks/infinite_water_cauldron/goggles
execute if entity @s[tag=ra.custom_block.infinite_lava_cauldron] run function ra_interactive:blocks/infinite_lava_cauldron/goggles
execute if entity @s[tag=ra.custom_block.infinite_snow_cauldron] run function ra_interactive:blocks/infinite_snow_cauldron/goggles
execute if entity @s[tag=ra.custom_block.item_mover] run function ra_interactive:blocks/item_mover/goggles
execute if entity @s[tag=ra.custom_block.item_pipe] run function ra_interactive:blocks/item_pipe/goggles
execute if entity @s[tag=ra.custom_block.message_block] run function ra_interactive:blocks/message_block/goggles
execute if entity @s[tag=ra.custom_block.spitter] run function ra_interactive:blocks/spitter/goggles
# ra_gates
execute if entity @s[tag=ra.custom_block.uni_gate] run function ra_gates:blocks/uni_gate/goggles
execute if entity @s[tag=ra.custom_block.clock] run function ra_gates:blocks/clock/goggles
execute if entity @s[tag=ra.custom_block.delayer] run function ra_gates:blocks/delayer/goggles
execute if entity @s[tag=ra.custom_block.extender] run function ra_gates:blocks/extender/goggles
execute if entity @s[tag=ra.custom_block.shortener] run function ra_gates:blocks/shortener/goggles
execute if entity @s[tag=ra.custom_block.randomizer] run function ra_gates:blocks/randomizer/goggles
# ra_storage
execute if entity @s[tag=ra.custom_block.boxer] run function ra_storage:blocks/boxer/goggles
execute if entity @s[tag=ra.custom_block.unboxer] run function ra_storage:blocks/unboxer/goggles
# ra_sensors
execute if entity @s[tag=ra.custom_block.entity_detector] run function ra_sensors:blocks/entity_detector/goggles
execute if entity @s[tag=ra.custom_block.tag_adder] run function ra_sensors:blocks/tag_adder/goggles
execute if entity @s[tag=ra.custom_block.tag_remover] run function ra_sensors:blocks/tag_remover/goggles
# ra_wireless
execute if entity @s[tag=ra.custom_block.emitter] run function ra_wireless:blocks/emitter/goggles
execute if entity @s[tag=ra.custom_block.receiver] run function ra_wireless:blocks/receiver/goggles
# ra_chunk_loader
execute if entity @s[tag=ra.custom_block.chunk_loader] run function ra_chunk_loader:blocks/chunk_loader/goggles
# ra_wires
execute if entity @s[tag=ra.custom_block.liquid_pipe] run function ra_wires:blocks/liquid_pipe/goggles
execute if entity @s[tag=ra.custom_block.liquid_tank] run function ra_wires:blocks/liquid_tank/goggles
execute if entity @s[tag=ra.custom_block.liquid_pump] run function ra_wires:blocks/liquid_pump/goggles
execute if entity @s[tag=ra.custom_block.liquid_valve] run function ra_wires:blocks/liquid_valve/goggles
execute if entity @s[tag=ra.custom_block.liquid_drain] run function ra_wires:blocks/liquid_drain/goggles
execute if entity @s[tag=ra.custom_block.gas_tank] run function ra_wires:blocks/gas_tank/goggles
execute if entity @s[tag=ra.custom_block.gas_pump] run function ra_wires:blocks/gas_pump/goggles
execute if entity @s[tag=ra.custom_block.gas_valve] run function ra_wires:blocks/gas_valve/goggles
execute if entity @s[tag=ra.custom_block.boiler] run function ra_wires:blocks/boiler/goggles
execute if entity @s[tag=ra.custom_block.electric_wire] run function ra_wires:blocks/electric_wire/goggles
execute if entity @s[tag=ra.custom_block.electric_generator] run function ra_wires:blocks/electric_generator/goggles
execute if entity @s[tag=ra.custom_block.electric_consumer] run function ra_wires:blocks/electric_consumer/goggles
execute if entity @s[tag=ra.custom_block.electric_switch] run function ra_wires:blocks/electric_switch/goggles
execute if entity @s[tag=ra.custom_block.solar_panel] run function ra_wires:blocks/solar_panel/goggles
execute if entity @s[tag=ra.custom_block.battery] run function ra_wires:blocks/battery/goggles
execute if entity @s[tag=ra.custom_block.industrial_light] run function ra_wires:blocks/industrial_light/goggles
execute if entity @s[tag=ra.custom_block.electric_breaker] run function ra_wires:blocks/electric_breaker/goggles
# ra_infinite
execute if entity @s[tag=ra.custom_block.mineral_generator] run function ra_infinite:blocks/mineral_generator/goggles
execute if entity @s[tag=ra.custom_block.nether_generator] run function ra_infinite:blocks/nether_generator/goggles
execute if entity @s[tag=ra.custom_block.poppy_generator] run function ra_infinite:blocks/poppy_generator/goggles

# ra_ender
execute if entity @s[tag=ra.custom_block.ender_item_vault] run function ra_ender:blocks/item_vault/goggles
execute if entity @s[tag=ra.custom_block.ender_fluid_vault] run function ra_ender:blocks/fluid_vault/goggles
execute if entity @s[tag=ra.custom_block.ender_power_vault] run function ra_ender:blocks/power_vault/goggles
execute if entity @s[tag=ra.custom_block.teleport_anchor] run function ra_ender:blocks/teleport_anchor/goggles
