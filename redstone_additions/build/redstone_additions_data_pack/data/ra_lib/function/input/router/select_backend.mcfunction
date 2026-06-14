# /ra_lib:input/router/select_backend
# Select backend for current mode.
# Auto strategy: numeric -> trigger, text -> writable_book.

scoreboard players set @s ra.input.backend 0

execute if score @s ra.input.mode matches 1 run scoreboard players set @s ra.input.backend 1
execute if score @s ra.input.mode matches 3 run scoreboard players set @s ra.input.backend 1
execute if score @s ra.input.mode matches 2 run scoreboard players set @s ra.input.backend 2

execute unless score @s ra.input.backend matches 1..2 run scoreboard players set @s ra.input.backend 1

execute store result storage ra:input tmp.req int 1 run scoreboard players get @s ra.input.req
execute store result storage ra:input tmp.backend int 1 run scoreboard players get @s ra.input.backend
function ra_lib:input/router/store_backend with storage ra:input tmp
data remove storage ra:input tmp
