# /ra_lib:transport/rebuild/adopt_medium_write {id:N,medium:"..."}
# Internal: set the network's medium if it does not have one yet.

$execute unless data storage ra:transport nets.n$(id).medium run data modify storage ra:transport nets.n$(id).medium set value "$(medium)"
