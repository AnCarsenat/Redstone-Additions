# /ra_settings:row/str {label,key,i,ie}
# Internal: a text row. Typed, not stepped, so it has one button and the value
# arrives on a later tick through ra_lib:input.
#
# The current value is resolved into cur.show before drawing, because it lives
# under the player's UUID and no text component can path to that.

function ra_settings:user_str with storage ra:settings cur
data modify storage ra:settings cur.show set from storage ra:settings out

$tellraw @s [{text:"  $(label): ",color:"white"},{text:"[",color:"dark_gray"},{nbt:"cur.show",storage:"ra:settings",color:"aqua"},{text:"]",color:"dark_gray"},{text:" "},{text:"[you]",color:"aqua"},{text:"  "},{text:"[ Modify ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Type a new value"},click_event:{action:"run_command",command:"/trigger ra.settings.act set $(ie)"}}]
