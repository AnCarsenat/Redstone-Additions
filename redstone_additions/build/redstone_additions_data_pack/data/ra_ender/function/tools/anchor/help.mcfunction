# /ra_ender:tools/anchor/help
# How to wire an anchor up, printed in chat.

tellraw @s [{text:"— Teleport Anchor ",color:"light_purple",bold:true},{text:"—",color:"dark_gray"}]
tellraw @s [{text:"Every anchor has a string ",color:"gray"},{text:"id",color:"aqua"},{text:" and a table of 15 target ids, one per redstone strength.",color:"gray"}]
tellraw @s [{text:"/function ra_ender:tools/anchor/set_id {id:\"A\"}",color:"yellow"},{text:" — name the nearest anchor",color:"gray"}]
tellraw @s [{text:"/function ra_ender:tools/anchor/set_table {table:[\"A\",\"B\",\"C\"]}",color:"yellow"},{text:" — the whole table at once",color:"gray"}]
tellraw @s [{text:"/function ra_ender:tools/anchor/set_target {level:3,id:\"B\"}",color:"yellow"},{text:" — one row",color:"gray"}]
tellraw @s [{text:"/function ra_ender:tools/anchor/show",color:"yellow"},{text:" — print the nearest anchor's table",color:"gray"}]
tellraw @s [{text:"Stand within 2 blocks and power it: strength picks the row.",color:"gray"}]
