# Thruster deadzone (cm/tick): type an exact value (0-200).
data modify storage ra:settings edit set value {kind:"global",key:"jetpack_deadzone",type:"int",min:0,max:200}
function ra_settings:edit_start
