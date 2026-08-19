# /ra_settings:user_str_at {u,key}
# Internal: overwrite the default with this player's value, if they have one.

$execute if data storage ra:settings users[{u:$(u)}]."$(key)" run data modify storage ra:settings out set from storage ra:settings users[{u:$(u)}]."$(key)"
