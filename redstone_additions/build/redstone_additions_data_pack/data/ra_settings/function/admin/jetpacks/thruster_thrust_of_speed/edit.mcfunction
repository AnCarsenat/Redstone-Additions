# Thruster thrust (% of speed): type an exact value (10-300).
data modify storage ra:settings edit set value {kind:"global",key:"jetpack_thrust",type:"int",min:10,max:300}
function ra_settings:edit_start
