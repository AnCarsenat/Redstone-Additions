# /ra_lib:transport/rebuild/absorb_potion {id:N}
# Internal: give the network the carried potion contents, if it has none yet.

$execute unless data storage ra:transport nets.n$(id).potion run data modify storage ra:transport nets.n$(id).potion set from storage ra:transport absq.potion
