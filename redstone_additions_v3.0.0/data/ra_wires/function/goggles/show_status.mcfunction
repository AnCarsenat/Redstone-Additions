# /ra_wires:goggles/show_status
# Add RA Wires specific status lines below the block name
# Context: as RA Wires marker, at marker position

# Merged fluid status
execute if entity @s[tag=ra.wires.fluid_node] run data modify storage ra:temp status_literal set value {y:1.05,label:"Medium: ",value_color:"aqua",suffix:""}
execute if entity @s[tag=ra.wires.fluid_node] run data modify storage ra:temp status_literal.value set from entity @s data.status.medium
execute if entity @s[tag=ra.wires.fluid_node] run function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal

execute if entity @s[tag=ra.wires.fluid_node] run data modify storage ra:temp status_literal set value {y:0.8,label:"Amount: ",value_color:"yellow",suffix:""}
execute if entity @s[tag=ra.wires.fluid_node] run data modify storage ra:temp status_literal.value set from entity @s data.status.amount
execute if entity @s[tag=ra.wires.fluid_node] run function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal

# Electric status
execute if entity @s[tag=ra.wires.electric_node] run data modify storage ra:temp status_literal set value {y:1.05,label:"EU: ",value_color:"aqua",suffix:""}
execute if entity @s[tag=ra.wires.electric_node] run data modify storage ra:temp status_literal.value set from entity @s data.status.available_eu
execute if entity @s[tag=ra.wires.electric_node] run function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal

execute if entity @s[tag=ra.wires.electric_node] run data modify storage ra:temp status_literal set value {y:0.8,label:"Active: ",value:"Off",value_color:"yellow",suffix:""}
execute if entity @s[tag=ra.wires.electric_node] if data entity @s data.status{active:1b} run data modify storage ra:temp status_literal.value set value "On"
execute if entity @s[tag=ra.wires.electric_node] run function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal

# Drain-specific line
execute if entity @s[tag=ra.custom_block.liquid_drain] run data modify storage ra:temp status_literal set value {y:0.55,label:"Drain: ",value_color:"gray",suffix:""}
execute if entity @s[tag=ra.custom_block.liquid_drain] run data modify storage ra:temp status_literal.value set from entity @s data.status.drain_state
execute if entity @s[tag=ra.custom_block.liquid_drain] run function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
