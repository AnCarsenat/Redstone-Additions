# /ra_multiblock:blocks/register_all
# Announce the multiblock bases on load, for anyone wearing the ra.debug tag.
#
# One block type with five tiers, so five lines: knowing the module loaded is not
# the same as knowing which tiers it knows about.

tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Copper Multiblock Base registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Iron Multiblock Base registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Gold Multiblock Base registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Diamond Multiblock Base registered",color:"gray"}]
tellraw @a[tag=ra.debug] [{text:"[RA] ",color:"gold"},{text:"Netherite Multiblock Base registered",color:"gray"}]
