# /ra_wires:bridge/match_medium {medium:"..."}
# Internal: does the destination hold the same medium the source is sending?
# Sets #br.dst to 1 when it does, and leaves it alone when it does not.
#
# A macro because the comparison is between two runtime strings, and a block
# state test cannot do that — the destination's medium arrives as the macro
# argument and the source's is read out of storage.

$execute if data storage ra:wires bridge{medium:"$(medium)"} run scoreboard players set #br.dst ra.wires.tmp 1
