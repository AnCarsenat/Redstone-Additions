# Delayer delay: type an exact value (1-1200).
data modify storage ra:settings admin_edit set value {block:"delayer",prop:"delay",type:"int",min:1,max:1200}
function ra_settings:admin_edit_start
