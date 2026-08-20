# /ra_settings:page/open_at {p:N}
# Internal: draw page N.
#
# A page may hand its own drawing function instead of a row list, for anything
# the generic renderer cannot express. That function is called as the player and
# is on its own from there.

$execute unless data storage ra:settings pages[$(p)] run return run tellraw @s [{text:"[Settings] ",color:"gold"},{text:"That page no longer exists — a module may have been removed.",color:"gray"}]

$data modify storage ra:settings cur set from storage ra:settings pages[$(p)]

# Drawing a menu is what makes its buttons usable; see ra_settings:init.
scoreboard players set @s ra.settings.viewing 1200

tellraw @s [{text:""},{text:"─── ",color:"dark_gray"},{nbt:"cur.title",storage:"ra:settings",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]

execute if data storage ra:settings cur.render run function ra_settings:page/custom with storage ra:settings cur
execute if data storage ra:settings cur.render run return 0

scoreboard players set #idx ra.set.tmp 0
$data modify storage ra:settings rowscan set from storage ra:settings pages[$(p)].rows
function ra_settings:page/rows
data remove storage ra:settings rowscan

tellraw @s [{text:"  "},{text:"[ Back ]",color:"gray",hover_event:{action:"show_text",value:"Back to all pages"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 1"}}]
