# /ra_wires:fluid/potion_store_run {id:N}
# Internal: the dynamic-name half of potion_store.

$execute unless data storage ra:transport nets.n$(id).potion run data modify storage ra:transport nets.n$(id).potion set from storage ra:wires pot.contents
