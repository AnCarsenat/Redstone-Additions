# Goggles range (blocks): type an exact value (4-64).
data modify storage ra:settings admin_edit set value {key:"goggles_range",type:"int",min:4,max:64}
function ra_settings:admin_edit_start
