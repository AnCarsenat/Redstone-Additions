# /ra_wires:bridge/tick
# Even out the two networks this block sits between.
# Context: as a bridge marker, at its block.
#
# WHAT A BRIDGE IS
# A bridge belongs to neither network. That is the whole point: a node belongs to
# exactly one network, so anything that joined would merge the two sides and there
# would be nothing left to bridge. The Boiler has always worked this way.
#
# Because it is not a member, it also breaks connectivity by simply existing: a
# pipe run with a valve in the middle is two networks, always, and the valve is
# the only thing that moves anything between them.
#
# IT NO LONGER CARES WHICH WAY IT FACES
# It used to bridge the block in front and the block behind, which meant a valve
# placed on an east-west pipe run while the player happened to be facing south
# pointed at two empty spaces and did nothing at all, with no way to tell from
# looking at it. Now it looks at all six neighbours and finds the networks itself.

execute if data entity @s data.properties{enabled:0b} run return run data modify entity @s data.status.bridge_state set value "disabled"

# Redstone is the on switch. ra_lib:redstone/any is the cheap reader: it stops at
# the first live side and never resolves a level, which is all that is wanted.
execute unless function ra_lib:redstone/any run return run data modify entity @s data.status.bridge_state set value "unpowered"

function ra_wires:bridge/scan
