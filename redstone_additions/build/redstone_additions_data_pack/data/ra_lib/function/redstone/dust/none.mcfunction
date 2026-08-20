# /ra_lib:redstone/dust/none {...}
# Internal: the side where dust is never an input -- the one below us.
#
# Dust powers the block underneath it, not the one above, so a line on the floor
# below does not reach us. Dust that climbs the side of our block is on a
# horizontal neighbour with its connection state set to `up`, and dust/side
# already reads that.
#
# Deliberately empty, so that this rule is written down rather than being an
# unexplained missing case. It exists for the same reason torch/none does.
