# /ra_wires:blocks/creative_fluid/known {medium:"water"}
# Internal: is this the name of a medium the pack knows? Returns 1 or 0.
#
# One lookup against the registry in ra_wires:media/init. Anything added there is
# accepted here without an edit, which is the point of the registry.

$execute if data storage ra:wires media.$(medium) run return 1
return 0
