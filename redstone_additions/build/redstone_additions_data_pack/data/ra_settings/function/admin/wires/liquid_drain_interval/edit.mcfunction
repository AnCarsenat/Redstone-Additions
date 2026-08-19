# Liquid Drain interval: type an exact value (1-1200).
data modify storage ra:settings admin_edit set value {block:"liquid_drain",prop:"cooldown",type:"int",min:1,max:1200}
function ra_settings:admin_edit_start
