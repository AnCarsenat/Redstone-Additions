# /ra:tools/input_data_handler/poll
# Display current input session state.

execute store result score @s ra.temp run function ra_lib:input/poll
tellraw @s [{text:"[RA Input] state=",color:"gold"},{score:{name:"@s",objective:"ra.temp"},color:"yellow"},{text:" (0=idle, 1=waiting, 2=ready)",color:"gray"}]