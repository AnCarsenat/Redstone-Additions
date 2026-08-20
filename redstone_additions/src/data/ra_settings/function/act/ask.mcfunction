# /ra_settings:act/ask
# Internal: describe this row as an edit and ask for a value. Context: as the player.

data modify storage ra:settings edit set value {kind:"user"}
data modify storage ra:settings edit.obj set from storage ra:settings cur.obj
data modify storage ra:settings edit.key set from storage ra:settings cur.key
data modify storage ra:settings edit.type set from storage ra:settings cur.type
execute if data storage ra:settings cur.min run data modify storage ra:settings edit.min set from storage ra:settings cur.min
execute if data storage ra:settings cur.max run data modify storage ra:settings edit.max set from storage ra:settings cur.max

function ra_settings:edit_start
