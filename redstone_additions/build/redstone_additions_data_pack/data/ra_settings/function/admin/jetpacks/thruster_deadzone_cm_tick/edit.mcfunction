# Thruster deadzone (cm/tick): type an exact value (0-200).
data modify storage ra:settings admin_edit set value {key:"jetpack_deadzone",type:"int",min:0,max:200}
function ra_settings:admin_edit_start
