# Allow Delayer to be placed again.
data remove storage ra:settings disabled[{b:"delayer"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Delayer",color:"white"},{text:" enabled.",color:"green"}]
