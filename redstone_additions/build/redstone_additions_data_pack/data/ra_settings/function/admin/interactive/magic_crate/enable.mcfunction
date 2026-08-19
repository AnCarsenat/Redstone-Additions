# Allow Magic Crate to be placed again.
data remove storage ra:settings disabled[{b:"magic_crate"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Magic Crate",color:"white"},{text:" enabled.",color:"green"}]
