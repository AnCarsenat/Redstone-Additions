# Current values on the Wireless Redstone page.
tellraw @s [{text:"─── ",color:"dark_gray"},{text:"Wireless Redstone",color:"gold",bold:true},{text:" ───",color:"dark_gray"}]
execute unless data storage ra:settings disabled[{b:"emitter"}] run tellraw @s [{text:"  Emitter: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"emitter"}] run tellraw @s [{text:"  Emitter: ",color:"white"},{text:"disabled",color:"red"}]
execute unless data storage ra:settings disabled[{b:"receiver"}] run tellraw @s [{text:"  Receiver: ",color:"white"},{text:"enabled",color:"green"}]
execute if data storage ra:settings disabled[{b:"receiver"}] run tellraw @s [{text:"  Receiver: ",color:"white"},{text:"disabled",color:"red"}]
