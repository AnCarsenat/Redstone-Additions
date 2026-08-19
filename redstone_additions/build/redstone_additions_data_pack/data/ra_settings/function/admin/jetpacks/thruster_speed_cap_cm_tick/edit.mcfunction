# Thruster speed cap (cm/tick): type an exact value (50-2000).
data modify storage ra:settings edit set value {kind:"global",key:"jetpack_speed_cap",type:"int",min:50,max:2000}
function ra_settings:edit_start
