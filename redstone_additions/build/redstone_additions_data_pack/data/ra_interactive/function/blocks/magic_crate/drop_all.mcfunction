# /ra_interactive:blocks/magic_crate/drop_all
# Spill the crate's contents when it is broken.
# Context: at the block, before the barrel is gone.
#
# Recursive rather than nine copies of the same three lines: the container is 27
# slots, and the written-out form other blocks use only ever covered the first
# nine of them.

execute unless data block ~ ~ ~ Items[0] run return 0

summon item ~ ~0.5 ~ {Tags:["ra","ra.mh_drop"]}
data modify entity @e[type=item,tag=ra.mh_drop,limit=1] Item set from block ~ ~ ~ Items[0]
tag @e[type=item,tag=ra.mh_drop] remove ra.mh_drop
data remove block ~ ~ ~ Items[0]

function ra_interactive:blocks/magic_crate/drop_all
