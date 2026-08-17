# /ra_lib:transport/mark_dirty
# Request a network rebuild. Call whenever a transport node is placed, broken or
# has its connectivity changed. Rebuilds are debounced by transport/tick, so
# placing a long pipe run in one tick still only costs one rebuild.

data modify storage ra:transport dirty set value 1b
