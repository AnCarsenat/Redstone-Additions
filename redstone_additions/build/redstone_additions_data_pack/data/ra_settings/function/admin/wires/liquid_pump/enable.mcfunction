# Allow Liquid Pump to be placed again.
data remove storage ra:settings disabled[{b:"liquid_pump"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Liquid Pump",color:"white"},{text:" enabled.",color:"green"}]
