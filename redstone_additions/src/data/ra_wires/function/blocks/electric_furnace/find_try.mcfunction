# /ra_wires:blocks/electric_furnace/find_try {id,slot}
# Internal: is the stack in this slot smeltable? Context: at the block.
#
# Every slot counts. Results leave the block entirely, so there is no output
# region to keep clear and no way for the furnace to find its own product.
#
# The slot goes into ef.hit as well as into the score, because take_input is a
# macro function reading ef.hit and needs $(slot) from it. Setting only the score
# is what made the furnace duplicate: take_input's macro had no `slot` to
# substitute, so the call failed outright and removed nothing, while #ef.took
# still held the 1 left by the previous successful smelt -- so deliver ran anyway
# and produced an ingot from an ore that was never consumed.

$data modify storage ra:wires ef.hit set from storage ra:wires smelt_map."$(id)"
execute unless data storage ra:wires ef.hit run return 0

$data modify storage ra:wires ef.hit.slot set value $(slot)
$scoreboard players set #ef.slot ra.wires.tmp $(slot)
