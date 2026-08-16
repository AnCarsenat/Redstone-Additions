# /ra_wires:fluid/particles {medium:"..."}
# Show the particle a medium is registered with, at the current position.

$execute if data storage ra:wires media.$(medium).particle run function ra_wires:fluid/particles_run with storage ra:wires media.$(medium)
