# Current values on the Multiblocks page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Multiblocks",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"multiblock_base"}] run tellraw @s [{text:"  Multiblock Base: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"multiblock_base"}] run tellraw @s [{text:"  Multiblock Base: ",color:"white"},{text:"disabled",color:"red"}]
