# Allow Mineral Generator to be placed again.
data remove storage ra:settings disabled[{b:"mineral_generator"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Mineral Generator",color:"white"},{text:" enabled.",color:"green"}]
