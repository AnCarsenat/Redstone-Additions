# Current values on the Chunk Loader page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Chunk Loader",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"chunk_loader"}] run tellraw @s [{text:"  Chunk Loader: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"chunk_loader"}] run tellraw @s [{text:"  Chunk Loader: ",color:"white"},{text:"disabled",color:"red"}]
