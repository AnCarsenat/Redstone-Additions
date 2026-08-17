# /ra_wires:blocks/register_all
# Announce every RA Wires block on load, for anyone wearing the ra.debug tag.
#
# This module keeps its per-block detail in one file rather than a folder each —
# the same reason handle_placement holds every spec — but a single "blocks
# registered" line for sixteen blocks told you nothing about which of them loaded.

tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Liquid Pipe registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Liquid Tank registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Liquid Pump registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Liquid Valve registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Liquid Drain registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Gas Pipe registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Gas Tank registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Gas Pump registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Gas Valve registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Boiler registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Electric Wire registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"EU Generator registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"EU Consumer registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"EU Switch registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Solar Panel registered",color:"gray"}]
