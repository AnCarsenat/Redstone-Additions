# Allow EU Switch to be placed again.
data remove storage ra:settings disabled[{b:"electric_switch"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"EU Switch",color:"white"},{text:" enabled.",color:"green"}]
