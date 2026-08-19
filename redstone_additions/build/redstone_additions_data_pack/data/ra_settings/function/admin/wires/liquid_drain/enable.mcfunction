# Allow Liquid Drain to be placed again.
data remove storage ra:settings disabled[{b:"liquid_drain"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Liquid Drain",color:"white"},{text:" enabled.",color:"green"}]
