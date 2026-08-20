# Randomizer chance %: type an exact value (0-100).
data modify storage ra:settings edit set value {kind:"prop",block:"randomizer",prop:"chance",type:"int",min:0,max:100}
function ra_settings:edit_start
