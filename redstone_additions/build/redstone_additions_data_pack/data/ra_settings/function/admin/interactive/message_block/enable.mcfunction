# Allow Message Block to be placed again.
data remove storage ra:settings disabled[{b:"message_block"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Message Block",color:"white"},{text:" enabled.",color:"green"}]
