# /ra_settings:apply_str_at {u,key}
# Internal: append this player's row if they have none, then write the value.

$execute unless data storage ra:settings users[{u:$(u)}] run data modify storage ra:settings users append value {u:$(u)}
$data modify storage ra:settings users[{u:$(u)}]."$(key)" set from storage ra:input consume.text
