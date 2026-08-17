# /ra_lib_multiblock:io/peek_here
# Internal: positioned at the IO container by io/at.

execute unless data block ~ ~ ~ Items[0] run return 0
data modify storage ra:multiblock io_item set from block ~ ~ ~ Items[0]
scoreboard players set #mb_io ra.multiblock 1
