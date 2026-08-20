# Allow Shortener to be placed again.
data remove storage ra:settings disabled[{b:"shortener"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Shortener",color:"white"},{text:" enabled.",color:"green"}]
