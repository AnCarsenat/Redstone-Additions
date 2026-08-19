# Thrust (% of speed): type an exact value (10-300).
data modify storage ra:settings admin_edit set value {key:"jetpack_thrust",type:"int",min:10,max:300}
function ra_settings:admin_edit_start
