# /ra_wires:blocks/electric_furnace/cooking
# Working, but not finishing an item this tick. Context: as the marker, at the block.
#
# Reached only once tick.mcfunction has established all three conditions: there is
# something smeltable in the barrel, the destination will take the result, and the
# grid is holding enough EU for one operation. That is what "cooking" means, so
# that is what the block is drawn as -- the cooldown only decides which of those
# ticks is the one an item actually comes out on.
#
# No particles and no sound: those mark a completed item and belong to `running`.
# Emitting them here would turn superpowered into a smoke column.

data modify entity @s data.status.state set value "Smelting"
function ra_wires:blocks/electric_furnace/stay_lit
