# Allow Industrial Light to be placed again.
data remove storage ra:settings disabled[{b:"industrial_light"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Industrial Light",color:"white"},{text:" enabled.",color:"green"}]
