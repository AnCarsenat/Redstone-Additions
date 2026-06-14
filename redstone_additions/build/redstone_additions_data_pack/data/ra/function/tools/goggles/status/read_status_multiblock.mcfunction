# /ra:tools/goggles/status/read_status_multiblock
# Display multiblock-specific status info (heat, enabled, etc.)
# Context: as multiblock marker entity, at base position

# Heat is scoreboard-backed for blast forge and updates live
data modify storage ra:temp status_literal set value {y:2.35,label:"Heat: ",value:"N/A",value_color:"gold",suffix:"/1000"}
execute if score @s ra.heat matches -2147483648..2147483647 store result storage ra:temp status_literal.value int 1 run scoreboard players get @s ra.heat
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal

# Enabled remains NBT-backed from multiblock properties
data modify storage ra:temp status_literal set value {y:2.1,label:"Enabled: ",value:"N/A",value_color:"aqua",suffix:""}
execute if data entity @s data.properties.enabled run data modify storage ra:temp status_literal.value set from entity @s data.properties.enabled
function ra:tools/goggles/billboard/show_literal_line with storage ra:temp status_literal
