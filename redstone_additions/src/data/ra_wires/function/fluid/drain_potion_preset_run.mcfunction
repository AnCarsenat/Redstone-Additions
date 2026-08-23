# /ra_wires:fluid/drain_potion_preset_run {potion:"minecraft:strength"}
# Internal: the dynamic-name half of drain_potion_preset. Rewrites the table's
# short field names into the same shape custom_effects uses, so the applier only
# ever sees one format.
#
# A potion with no entry -- water, mundane, thick, awkward, or one added by a
# future version -- leaves the list absent and nothing is applied.

$execute unless data storage ra:wires potion_effects."$(potion)" run return 0
$data modify storage ra:wires eff.list set from storage ra:wires potion_effects."$(potion)"
