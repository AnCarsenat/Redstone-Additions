# /ra_lib:transport/net/migrate_pick {id:N}
# Internal: read the old single-medium shape off a network, so migrate_run can be
# handed both the id and the medium as macro arguments.
#
# Two steps because a macro line is substituted once: the medium is itself read
# out of a path that needs the id substituted into it, so it cannot also be a
# macro argument of the same line.

$data modify storage ra:transport mig set value {id:$(id)}
$data modify storage ra:transport mig.medium set from storage ra:transport nets.n$(id).medium
function ra_lib:transport/net/migrate_run with storage ra:transport mig
