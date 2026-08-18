# /ra:tools/multimeter/off_network
# Internal: this block is not a transport node at all.
#
# Three cases reach here and they are worth telling apart, because "0 of 0" looks
# identical to a flat battery: a bridge belongs to no network by design, a switch
# that has been turned off has left its own, and everything else in the pack was
# simply never a node.

execute if entity @s[tag=ra.wires.bridge] run return run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  A bridge belongs to no network — it moves between the two beside it.",color:"gray"}]
execute if entity @s[tag=ra.custom_block.electric_switch] run return run tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Switched off, so it has left its grid.",color:"gray"}]
tellraw @a[tag=ra.meter_active,limit=1] [{text:"  Not a network block.",color:"gray"}]
