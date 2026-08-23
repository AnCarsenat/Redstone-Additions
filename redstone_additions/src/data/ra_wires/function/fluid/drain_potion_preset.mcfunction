# /ra_wires:fluid/drain_potion_preset {id:N}
# Internal: look a preset potion's effects up in the table.
# Two steps, because the potion's own id has to be substituted into a path that
# already needed the network id substituted into it.

data remove storage ra:wires pre
$data modify storage ra:wires pre.potion set from storage ra:transport nets.n$(id).potion.potion
execute unless data storage ra:wires pre.potion run return 0
function ra_wires:fluid/drain_potion_preset_run with storage ra:wires pre
