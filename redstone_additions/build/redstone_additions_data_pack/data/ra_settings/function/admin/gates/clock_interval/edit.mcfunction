# Clock interval: type an exact value (2-1200).
data modify storage ra:settings edit set value {kind:"prop",block:"clock",prop:"cooldown",type:"int",min:2,max:1200}
function ra_settings:edit_start
