# Chat prefix: type a new value.
data modify storage ra:settings edit set value {kind:"global",key:"prefix",type:"str"}
function ra_settings:edit_start
