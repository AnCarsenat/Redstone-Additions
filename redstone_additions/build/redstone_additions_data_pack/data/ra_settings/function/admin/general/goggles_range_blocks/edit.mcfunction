# Goggles range (blocks): type an exact value (4-64).
data modify storage ra:settings edit set value {kind:"global",key:"goggles_range",type:"int",min:4,max:64}
function ra_settings:edit_start
