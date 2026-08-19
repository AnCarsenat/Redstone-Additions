# Industrial Light EU: type an exact value (0-1000).
data modify storage ra:settings admin_edit set value {block:"industrial_light",prop:"eu_use",type:"int",min:0,max:1000}
function ra_settings:admin_edit_start
