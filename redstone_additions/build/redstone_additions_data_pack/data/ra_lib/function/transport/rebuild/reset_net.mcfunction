# /ra_lib:transport/rebuild/reset_net {id:N}
# Internal: zero a freshly created network's totals.
# amounts and media start empty rather than absent, so nothing downstream has to
# tell "no breakdown yet" apart from "migrated and holding nothing".

$data modify storage ra:transport nets.n$(id) set value {amount:0,capacity:0,amounts:{},media:[]}
