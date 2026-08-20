# Allow Battery to be placed again.
data remove storage ra:settings disabled[{b:"battery"}]
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Battery",color:"white"},{text:" enabled.",color:"green"}]
