# /ra_interactive:blocks/magic_crate/sweep {r}
# Internal: reach out and take. Context: as the marker, at the crate block.
#
# `execute as` changes who is running, not where it is running, so inside `pull`
# @s is the item while the position is still the crate — which is exactly the
# pair of contexts the insert needs, with no position juggling.
#
# The limit caps the work one pulse can do. A crate standing in a mob farm's
# drop pile would otherwise walk every item entity in a 20 block sphere in a
# single tick; eight per pulse still clears a pile quickly and never spikes.

$execute as @e[type=item,distance=..$(r),sort=nearest,limit=8] run function ra_interactive:blocks/magic_crate/pull
