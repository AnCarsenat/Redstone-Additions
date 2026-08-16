# /ra_wires:liquid/pipe_vanished
# The conduit at a pipe marker is gone. Tear the displays down and record it.
# Context: as marker, at position.
#
# Note this runs before break detection in the same tick, so a genuinely broken
# pipe passes through here first and then through liquid/break/pipe.

function ra_wires:common/update_pipe_displays
function ra_wires:common/update_model_local_and_neighbors
tag @s remove ra.wires.pipe_present
