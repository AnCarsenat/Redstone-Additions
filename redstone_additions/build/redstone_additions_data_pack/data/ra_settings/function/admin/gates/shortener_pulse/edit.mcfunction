# Shortener pulse: type an exact value (1-200).
data modify storage ra:settings edit set value {kind:"prop",block:"shortener",prop:"pulse",type:"int",min:1,max:200}
function ra_settings:edit_start
