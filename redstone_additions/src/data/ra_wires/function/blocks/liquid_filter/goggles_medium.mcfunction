# /ra_wires:blocks/liquid_filter/goggles_medium
# Internal: draw the "Passing:" line, in the medium's own display name.
# Context: as the filter marker.

execute unless data entity @s data.properties.filter_medium run return run function ra:tools/goggles/billboard/text_line {label:"Passing: ",value:"Anything",color:"gray",suffix:"",y:0.8}

data modify storage ra:wires lq set value {}
data modify storage ra:wires lq.medium set from entity @s data.properties.filter_medium
function ra_wires:blocks/liquid_filter/goggles_medium_run with storage ra:wires lq
