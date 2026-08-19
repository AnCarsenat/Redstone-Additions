# Allow Nether Generator to be placed again.
data remove storage ra:settings disabled[{b:"nether_generator"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Nether Generator",color:"white"},{text:" enabled.",color:"green"}]
