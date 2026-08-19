# Message Block range: type an exact value (1-128).
data modify storage ra:settings edit set value {kind:"prop",block:"message_block",prop:"range",type:"int",min:1,max:128}
function ra_settings:edit_start
