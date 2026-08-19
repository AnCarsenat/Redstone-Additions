# Current values on the Storage page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Storage",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"boxer"}] run tellraw @s [{text:"  Boxer: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"boxer"}] run tellraw @s [{text:"  Boxer: ",color:"white"},{text:"disabled",color:"red"}]
execute unless data storage ra:settings disabled[{b:"unboxer"}] run tellraw @s [{text:"  Unboxer: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"unboxer"}] run tellraw @s [{text:"  Unboxer: ",color:"white"},{text:"disabled",color:"red"}]
