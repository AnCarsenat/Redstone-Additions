# Allow EU Consumer to be placed again.
data remove storage ra:settings disabled[{b:"electric_consumer"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"EU Consumer",color:"white"},{text:" enabled.",color:"green"}]
