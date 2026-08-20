# /ra_settings:placement/seed
# Copy the configured defaults onto a freshly placed block.
# Context: as the new marker.
#
# WHY HERE AND NOT IN ra_lib:util/property
# property() is read by every consumer, every bridge, every drain, on every tick,
# and it is four commands with no storage and no sub-calls on purpose. Teaching
# it to consult a settings table would put that lookup in the hottest path in the
# pack to answer a question that only changes when a block is placed.
#
# Applying it at placement also gives "default" the meaning an admin expects: new
# blocks come out with the configured value, and machines already standing in the
# world are not silently re-tuned underneath a build that was balanced around
# them. Someone who wants an old block changed can still wrench it.

execute unless data entity @s data.type run return 0
data modify storage ra:settings q.t set from entity @s data.type
function ra_settings:placement/seed_at with storage ra:settings q
