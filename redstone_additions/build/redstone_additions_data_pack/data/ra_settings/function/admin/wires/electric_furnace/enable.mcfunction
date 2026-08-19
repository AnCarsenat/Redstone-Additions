# Allow Electric Furnace to be placed again.
data remove storage ra:settings disabled[{b:"electric_furnace"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Electric Furnace",color:"white"},{text:" enabled.",color:"green"}]
