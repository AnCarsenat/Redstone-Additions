# /data/ra_wires/function/tick.mcfunction
# Tick merged fluid and electric systems in RA Wires

execute unless data storage ra:wires {initialized:1b} run function ra_wires:load

# Keep marker anchors snapped to centered block coordinates.
execute as @e[type=marker,tag=ra.wires.node] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~

function ra_wires:common/tick_cleanup
function ra_wires:liquid/tick
function ra_wires:electric/tick
