# Goggles redraw (ticks): type an exact value (5-100).
data modify storage ra:settings admin_edit set value {key:"goggles_redraw",type:"int",min:5,max:100}
function ra_settings:admin_edit_start
