# /ra_wires:blocks/liquid_filter/goggles_medium_run {medium:"water"}
# Internal: the dynamic-name half of goggles_medium.
#
# A medium the registry does not know still gets a line, showing the raw key.
# That is deliberate: a filter set to a misspelt medium would otherwise look
# identical to one set correctly and pass nothing, with nothing on the block to
# say why.

$execute unless data storage ra:wires media.$(medium) run return run function ra:tools/goggles/billboard/text_line {label:"Passing: ",value:"$(medium) (unknown)",color:"red",suffix:"",y:0.8}

data remove storage ra:wires lqline
$data modify storage ra:wires lqline.value set from storage ra:wires media.$(medium).name
$data modify storage ra:wires lqline.color set from storage ra:wires media.$(medium).color
data modify storage ra:wires lqline.label set value "Passing: "
data modify storage ra:wires lqline.suffix set value ""
data modify storage ra:wires lqline.y set value 0.8
function ra:tools/goggles/billboard/text_line with storage ra:wires lqline
