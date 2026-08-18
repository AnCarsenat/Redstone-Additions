# /ra_wires:electric/tick
# Tick the electric grid.
#
# WHAT CHANGED
# Electric used to be the last system still moving charge node to node: every
# wire held its own buffer and, once a tick, handed half the difference to one
# neighbour that had less. That model cannot deliver power, only level it out.
# Charge crawled one block per tick, a generator with six neighbours fed exactly
# one of them, and once a run had evened out to within 1 EU everywhere the
# transfer guard stopped it moving at all — so a consumer at the end of a line
# was fed by whatever leaked past the wires in front of it, if anything.
#
# It now uses the same network engine the fluid side has used since v5.1.4, on
# the `electric` class the engine already reserved. Adjacent nodes are flood
# filled into a grid, the charge belongs to the grid rather than to any block,
# and capacity is the sum of what each node contributes. A generator offers its
# output to the grid and a consumer takes from it, both in the same tick, however
# far apart they are. There is no propagation left to go wrong, no order
# dependence, and wires cost nothing per tick because there is nothing for them
# to do but conduct.

# Keep electric wire conduits non-waterlogged.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]

# Self-heal missing wire displays without forcing per-tick full rebuilds.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.wire_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors

# Break detection — straight to the cleanup handler, no ra.broken round-trip.
execute as @e[type=marker,tag=ra.custom_block.electric_wire] at @s unless block ~ ~ ~ conduit run function ra_wires:electric/break/wire
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s unless block ~ ~ ~ barrel run function ra_wires:electric/break/generator
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s unless block ~ ~ ~ observer run function ra_wires:electric/break/consumer
execute as @e[type=marker,tag=ra.custom_block.electric_switch] at @s unless block ~ ~ ~ waxed_cut_copper run function ra_wires:electric/break/switch
execute as @e[type=marker,tag=ra.custom_block.solar_panel] at @s unless block ~ ~ ~ daylight_detector run function ra_wires:electric/break/solar_panel
execute as @e[type=marker,tag=ra.custom_block.battery] at @s unless block ~ ~ ~ waxed_copper_grate run function ra_wires:electric/break/battery
execute as @e[type=marker,tag=ra.custom_block.electric_breaker] at @s unless block ~ ~ ~ waxed_copper_bulb run function ra_wires:electric/break/breaker
execute as @e[type=marker,tag=ra.custom_block.industrial_light] at @s unless block ~ ~ ~ sea_lantern run function ra_wires:electric/break/industrial_light

# Membership. Every one of these lines only fires when a node's membership
# disagrees with what it should be, which matters more than it looks: joining and
# leaving both mark the network dirty, so a line that fired unconditionally would
# force a rebuild every single tick for ever.
#
# Repaint a generator's furnace skin if it has gone missing. Anchored to the block
# centre, matching where ra_lib:skin/spawn stands its displays.
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s align xyz positioned ~0.5 ~0.5 ~0.5 unless entity @e[type=block_display,tag=ra.skin.electric_generator,distance=..0.4,limit=1] run function ra_wires:blocks/electric_generator/refresh_display

# Enrols anything placed before electric moved onto the network engine, and costs
# one tag test per node on every tick after that.
execute as @e[type=marker,tag=ra.wires.electric_node,tag=!ra.tr.node,tag=!ra.custom_block.electric_switch] at @s run function ra_wires:electric/adopt

# A switch is the exception: its enabled state decides whether it conducts, so it
# joins and leaves as that changes, however it was changed — goggles tinker, Data
# Handler toggle or a raw /data edit. Leaving severs the run at this block and
# the two halves rebuild as separate grids, which is what the old model could
# never really do: a switch there just declined to hand charge on, while still
# holding a buffer either side could pull from.
#
# Only `enabled` is allowed to keep a node off the grid. A disabled consumer or
# generator stops drawing or producing but still conducts, exactly as before —
# cutting a line is the switch's job alone.
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=!ra.tr.node] if data entity @s data.properties{enabled:1b} at @s run function ra_wires:electric/adopt
execute as @e[type=marker,tag=ra.custom_block.electric_switch,tag=ra.tr.node] unless data entity @s data.properties{enabled:1b} run function ra_lib:transport/net/leave

# Production, then draw. Order matters only in that a generator's output is
# spendable on the same tick it is made; nothing depends on which generator or
# which consumer the selector reaches first, because they all talk to the grid
# rather than to each other.
execute as @e[type=marker,tag=ra.custom_block.electric_generator] at @s run function ra_wires:electric/generator_tick
execute as @e[type=marker,tag=ra.custom_block.solar_panel] at @s run function ra_wires:blocks/solar_panel/tick
execute as @e[type=marker,tag=ra.custom_block.electric_consumer] at @s run function ra_wires:electric/consumer_tick
execute as @e[type=marker,tag=ra.custom_block.industrial_light] at @s run function ra_wires:blocks/industrial_light/tick

# Status refresh. Wires and switches do no other work.
execute as @e[type=marker,tag=ra.wires.electric_node] at @s run function ra_wires:electric/update_status
