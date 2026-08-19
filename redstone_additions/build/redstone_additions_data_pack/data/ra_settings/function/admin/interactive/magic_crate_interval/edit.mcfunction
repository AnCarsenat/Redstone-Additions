# Magic Crate interval: type an exact value (1-1200).
data modify storage ra:settings edit set value {kind:"prop",block:"magic_crate",prop:"cooldown",type:"int",min:1,max:1200}
function ra_settings:edit_start
