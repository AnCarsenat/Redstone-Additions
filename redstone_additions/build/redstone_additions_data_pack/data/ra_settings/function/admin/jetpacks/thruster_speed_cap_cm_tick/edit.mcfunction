# Thruster speed cap (cm/tick): type an exact value (50-2000).
data modify storage ra:settings admin_edit set value {key:"jetpack_speed_cap",type:"int",min:50,max:2000}
function ra_settings:admin_edit_start
