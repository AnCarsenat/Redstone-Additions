# Magic Crate radius: type an exact value (5-32).
data modify storage ra:settings admin_edit set value {block:"magic_crate",prop:"radius",type:"int",min:5,max:32}
function ra_settings:admin_edit_start
