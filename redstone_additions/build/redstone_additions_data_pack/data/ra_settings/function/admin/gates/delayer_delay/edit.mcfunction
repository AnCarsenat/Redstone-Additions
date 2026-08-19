# Delayer delay: type an exact value (1-1200).
data modify storage ra:settings edit set value {kind:"prop",block:"delayer",prop:"delay",type:"int",min:1,max:1200}
function ra_settings:edit_start
