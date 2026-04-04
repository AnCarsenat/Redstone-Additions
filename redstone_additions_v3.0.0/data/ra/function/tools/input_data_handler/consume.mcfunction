# /ra:tools/input_data_handler/consume
# Consume and display the latest input result.

execute store result score @s ra.temp run function ra_lib:input/consume
execute if score @s ra.temp matches 1 run tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Consumed: ",color:"green"},{nbt:"consume",storage:"ra:input",color:"white"}]
execute unless score @s ra.temp matches 1 run tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"No ready result.",color:"red"}]