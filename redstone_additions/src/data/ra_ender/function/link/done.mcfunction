# /ra_ender:link/done
# Drop the self tag at the end of a vault's cycle, and on every early return out
# of one. Returns 0 so it can be used as `return run function ...`.
# Context: as the vault marker.

tag @s remove ra.ender.self
return 0
