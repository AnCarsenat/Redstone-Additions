# Allow Item Mover to be placed again.
data remove storage ra:settings disabled[{b:"item_mover"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Item Mover",color:"white"},{text:" enabled.",color:"green"}]
