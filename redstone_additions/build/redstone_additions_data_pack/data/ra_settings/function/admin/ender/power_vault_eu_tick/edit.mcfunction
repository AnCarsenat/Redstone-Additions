# Power Vault EU/tick: type an exact value (1-10000).
data modify storage ra:settings admin_edit set value {block:"ender_power_vault",prop:"transfer_rate",type:"int",min:1,max:10000}
function ra_settings:admin_edit_start
