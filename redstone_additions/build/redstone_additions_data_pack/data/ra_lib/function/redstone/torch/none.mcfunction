# /ra_lib:redstone/torch/none {...}
# Internal: the side where no torch can ever be an input — the one above us.
#
# A torch up there is either standing ON this block, in which case it is attached
# to us and a torch never powers what it is mounted on, or it is a wall torch
# powering the block above itself, which is not us either way.
#
# Deliberately empty. It exists so ra_lib:redstone/side can dispatch on a name
# instead of branching, and so that this rule is written down somewhere rather
# than being an unexplained missing case. The old library powered a block from a
# torch standing on top of it, which is not how vanilla behaves.
