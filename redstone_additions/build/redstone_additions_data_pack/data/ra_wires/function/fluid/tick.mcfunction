# /ra_wires:fluid/tick
# Tick the merged fluid and gas system.
#
# Pipes, tanks and valves do no per-tick work. They contribute capacity and
# connectivity, and the contents belong to the network, so a hundred-block pipe
# run costs nothing to keep running. Only the blocks that actually move fluid
# between the world and the network are ticked.
#
# The previous design gave every node its own buffer and had each one push at its
# six neighbours every tick, guarded by a one-move-per-tick latch. Fluid crawled
# one block per tick, the result depended on which node the entity selector
# happened to reach first, and every node re-read and rewrote its NBT each tick
# whether anything was flowing or not.

# --- Pipe visuals ---
# Displays are only rebuilt on the tick a conduit appears or disappears at a pipe
# marker, or when a pipe's display entity has gone missing. They are never
# rebuilt on a tick where nothing changed.
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit[waterlogged=true] run setblock ~ ~ ~ conduit[waterlogged=false]
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=!ra.wires.pipe_present] at @s if block ~ ~ ~ conduit run function ra_wires:liquid/pipe_appeared
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe,tag=ra.wires.pipe_present] at @s unless block ~ ~ ~ conduit run function ra_wires:liquid/pipe_vanished
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s if block ~ ~ ~ conduit unless entity @e[type=block_display,tag=ra.wires.pipe_display.center,distance=..0.6,limit=1] run function ra_wires:common/update_model_local_and_neighbors

# --- Break detection ---
execute as @e[type=marker,tag=ra.custom_block.liquid_pipe] at @s unless block ~ ~ ~ conduit run function ra_wires:liquid/break/pipe
execute as @e[type=marker,tag=ra.custom_block.liquid_tank] at @s unless block ~ ~ ~ waxed_copper_block run function ra_wires:liquid/break/tank
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s unless block ~ ~ ~ dispenser run function ra_wires:liquid/break/pump
execute as @e[type=marker,tag=ra.custom_block.liquid_valve] at @s unless block ~ ~ ~ waxed_cut_copper run function ra_wires:liquid/break/valve
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s unless block ~ ~ ~ dropper run function ra_wires:liquid/break/drain
execute as @e[type=marker,tag=ra.custom_block.gas_tank] at @s unless block ~ ~ ~ iron_block run function ra_wires:liquid/break/gas_tank
execute as @e[type=marker,tag=ra.custom_block.gas_pump] at @s unless block ~ ~ ~ smoker run function ra_wires:liquid/break/gas_pump
execute as @e[type=marker,tag=ra.custom_block.gas_valve] at @s unless block ~ ~ ~ smooth_basalt run function ra_wires:liquid/break/gas_valve
execute as @e[type=marker,tag=ra.custom_block.boiler] at @s unless block ~ ~ ~ furnace run function ra_wires:liquid/break/boiler

# Valves are bridges now: they belong to no network and pump between the two they
# sit between. Two things have to be true of a valve built by an older version.
#
# It must stop being a member -- a valve that stayed in a network would merge the
# very runs it is supposed to keep apart, and the bridge would then find the same
# network on both sides and refuse to move anything.
execute as @e[type=marker,tag=ra.custom_block.liquid_valve,tag=ra.tr.node] run function ra_lib:transport/net/leave
execute as @e[type=marker,tag=ra.custom_block.gas_valve,tag=ra.tr.node] run function ra_lib:transport/net/leave

# And it must be tagged as a bridge so ra_wires:tick drives it at all. Idempotent,
# so this costs one tag write on a set that is almost always empty.
tag @e[type=marker,tag=ra.custom_block.liquid_valve,tag=!ra.wires.bridge] add ra.wires.bridge
tag @e[type=marker,tag=ra.custom_block.gas_valve,tag=!ra.wires.bridge] add ra.wires.bridge

# --- World <-> network ---
execute as @e[type=marker,tag=ra.custom_block.liquid_pump] at @s run function ra_wires:fluid/pump_tick
execute as @e[type=marker,tag=ra.custom_block.gas_pump] at @s run function ra_wires:fluid/pump_tick
execute as @e[type=marker,tag=ra.custom_block.liquid_drain] at @s run function ra_wires:fluid/drain_tick
execute as @e[type=marker,tag=ra.custom_block.boiler] at @s run function ra_wires:fluid/boiler_tick

# --- Status for the goggles, refreshed every 20 ticks ---
# The goggles only re-read billboards every 40 ticks, so writing these values on
# every node on every tick was pure waste.
scoreboard players add #status_timer ra.wires.tmp 1
execute if score #status_timer ra.wires.tmp matches 20.. run function ra_wires:fluid/refresh_status_all
