# Randomizer chance %: type an exact value (0-100).
data modify storage ra:settings admin_edit set value {block:"randomizer",prop:"chance",type:"int",min:0,max:100}
function ra_settings:admin_edit_start
