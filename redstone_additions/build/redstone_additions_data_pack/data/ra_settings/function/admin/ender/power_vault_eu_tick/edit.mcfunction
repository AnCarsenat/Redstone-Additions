# Power Vault EU/tick: type an exact value (1-10000).
data modify storage ra:settings edit set value {kind:"prop",block:"ender_power_vault",prop:"transfer_rate",type:"int",min:1,max:10000}
function ra_settings:edit_start
