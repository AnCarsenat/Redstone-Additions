# Generator EU/tick: type an exact value (1-10000).
data modify storage ra:settings edit set value {kind:"prop",block:"electric_generator",prop:"generation_rate",type:"int",min:1,max:10000}
function ra_settings:edit_start
