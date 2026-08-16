# /ra_wires:liquid/pipe_appeared
# A conduit showed up at a pipe marker that did not have one. Rebuild the local
# model and record the new state. Context: as marker, at position.

function ra_wires:common/update_model_local_and_neighbors
tag @s add ra.wires.pipe_present
