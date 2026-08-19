# Allow Infinite Snow Cauldron to be placed again.
data remove storage ra:settings disabled[{b:"infinite_snow_cauldron"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Infinite Snow Cauldron",color:"white"},{text:" enabled.",color:"green"}]
