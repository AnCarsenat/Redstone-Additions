# Shortener pulse: type an exact value (1-200).
data modify storage ra:settings admin_edit set value {block:"shortener",prop:"pulse",type:"int",min:1,max:200}
function ra_settings:admin_edit_start
