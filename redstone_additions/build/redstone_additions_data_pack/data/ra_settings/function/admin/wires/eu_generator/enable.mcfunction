# Allow EU Generator to be placed again.
data remove storage ra:settings disabled[{b:"electric_generator"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"EU Generator",color:"white"},{text:" enabled.",color:"green"}]
