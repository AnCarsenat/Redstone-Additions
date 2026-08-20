# Allow Infinite Water Cauldron to be placed again.
data remove storage ra:settings disabled[{b:"infinite_water_cauldron"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Infinite Water Cauldron",color:"white"},{text:" enabled.",color:"green"}]
