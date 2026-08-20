# Allow Infinite Lava Cauldron to be placed again.
data remove storage ra:settings disabled[{b:"infinite_lava_cauldron"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Infinite Lava Cauldron",color:"white"},{text:" enabled.",color:"green"}]
