# /ra_lib:input/backend/writable_book/give_book_safe
# Macro storage shape: {req:<int>}

# ra.temp: 1 when at least one main inventory/hotbar slot is empty.
scoreboard players set @s ra.temp 0
execute unless data entity @s Inventory[{Slot:0b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:1b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:2b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:3b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:4b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:5b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:6b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:7b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:8b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:9b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:10b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:11b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:12b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:13b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:14b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:15b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:16b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:17b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:18b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:19b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:20b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:21b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:22b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:23b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:24b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:25b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:26b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:27b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:28b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:29b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:30b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:31b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:32b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:33b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:34b}] run scoreboard players set @s ra.temp 1
execute unless data entity @s Inventory[{Slot:35b}] run scoreboard players set @s ra.temp 1

$execute if score @s ra.temp matches 1 run give @s minecraft:writable_book[item_name="Input Form",lore=[{text:"Write text on page 1",italic:false,color:"gray"},{text:"Close the book to submit",italic:false,color:"gray"}],custom_data={ra:{input_book:1b,input_req:$(req)}}]

$execute if score @s ra.temp matches 1 run data remove storage ra:input sessions.req_$(req).inventory_full_warned
$execute if score @s ra.temp matches 0 unless data storage ra:input sessions.req_$(req).inventory_full_warned run tellraw @s [{text:"[RA Input] ",color:"gold"},{text:"Inventory full. Free one slot for Input Form.",color:"red"}]
$execute if score @s ra.temp matches 0 run data modify storage ra:input sessions.req_$(req).inventory_full_warned set value 1b
