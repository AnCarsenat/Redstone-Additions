# /ra_lib:transport/rebuild/reset_net {id:N}
# Internal: zero a freshly created network's totals.

$data modify storage ra:transport nets.n$(id) set value {amount:0,capacity:0}
