# /data/ra/function/uninstall/confirm.mcfunction
# Full cleanup — removes ALL Redstone Additions data from the world
# Called via clickable [CONFIRM] button from ra:uninstall

# === Kill all custom block entities ===
kill @e[type=marker,tag=ra.custom_block]

# === Kill all multiblock markers ===
kill @e[type=marker,tag=ra.multiblock]

# === Kill all goggle billboards and display entities ===
kill @e[type=text_display,tag=ra.billboard]
kill @e[type=text_display,tag=ra.display]

# === Remove ALL scoreboards ===
scoreboard objectives remove ra.edit_type
scoreboard objectives remove ra.edit_step
scoreboard objectives remove ra.dh.action
scoreboard objectives remove ra.dh.pending
scoreboard objectives remove ra.input_handler.action
scoreboard objectives remove ra.cooldown
scoreboard objectives remove ra.filter_cd
scoreboard objectives remove ra.temp
scoreboard objectives remove ra.facing
scoreboard objectives remove ra.yaw
scoreboard objectives remove ra.pitch
scoreboard objectives remove ra.power
scoreboard objectives remove ra.power.up
scoreboard objectives remove ra.power.down
scoreboard objectives remove ra.power.north
scoreboard objectives remove ra.power.south
scoreboard objectives remove ra.power.east
scoreboard objectives remove ra.power.west
scoreboard objectives remove ra.power.front
scoreboard objectives remove ra.power.back
scoreboard objectives remove ra.power.left
scoreboard objectives remove ra.power.right
scoreboard objectives remove ra.power.local_up
scoreboard objectives remove ra.power.local_down
scoreboard objectives remove ra.inv.slot
scoreboard objectives remove ra.inv.count
scoreboard objectives remove ra.channel
scoreboard objectives remove ra.pulse_timer
scoreboard objectives remove ra.remote.pending
scoreboard objectives remove ra.remote.slot
scoreboard objectives remove ra.multiblock
scoreboard objectives remove ra.mb_timer
scoreboard objectives remove ra.mb_enabled
scoreboard objectives remove ra.craft_id
scoreboard objectives remove ra.heat
scoreboard objectives remove ra.input.state
scoreboard objectives remove ra.input.backend
scoreboard objectives remove ra.input.mode
scoreboard objectives remove ra.input.min
scoreboard objectives remove ra.input.max
scoreboard objectives remove ra.input.result
scoreboard objectives remove ra.input.ttl
scoreboard objectives remove ra.input.req
scoreboard objectives remove ra.input.next
scoreboard objectives remove ra.input.last
scoreboard objectives remove ra.input.value
scoreboard objectives remove ra.input.trigger
scoreboard objectives remove ra.wires.tmp
scoreboard objectives remove ra.wires.tmp2
scoreboard objectives remove ra.wrench
scoreboard objectives remove ra.wr.x
scoreboard objectives remove ra.wr.y
scoreboard objectives remove ra.wr.z

# === Clear data storage ===
data remove storage ra:multiblock {}
data remove storage ra:temp {}
data remove storage ra:block {}
data remove storage ra:cdh {}
data remove storage ra:dh {}
data remove storage ra:input {}
data remove storage ra:wires {}

# === Cancel scheduled ticks ===
schedule clear ra:tick

# === Remove player-held tags ===
tag @a remove ra.wrench_active
tag @a remove ra.cdh_active
tag @a remove ra.dh_active
tag @a remove ra.goggles_active
tag @a remove ra.wrench_clicked
tag @a remove ra.cdh_clicked
tag @a remove ra.dh_clicked
tag @a remove ra.remote_active
tag @a remove ra.remote_clicked
tag @a remove ra.input_handler_active
tag @a remove ra.input_handler_clicked
tag @a remove ra.input.active
tag @a remove ra.debug
tag @e[type=marker,tag=ra.dh_target] remove ra.dh_target
tag @a remove ra.wrench_user
tag @e[type=marker,tag=ra.wires.node] remove ra.wires.node
tag @e[type=marker,tag=ra.wrench.sel] remove ra.wrench.sel
tag @e[type=marker,tag=ra.wires.fluid_node] remove ra.wires.fluid_node
tag @e[type=marker,tag=ra.wires.liquid_node] remove ra.wires.liquid_node
tag @e[type=marker,tag=ra.wires.gas_node] remove ra.wires.gas_node
tag @e[type=marker,tag=ra.wires.electric_node] remove ra.wires.electric_node
tag @e[type=marker,tag=ra.wires.legacy_gas_pipe] remove ra.wires.legacy_gas_pipe
kill @e[type=block_display,tag=ra.skin]
kill @e[type=block_display,tag=ra.wires.pipe_display]
kill @e[type=block_display,tag=ra.wires.wire_display]

# === Final message_block ===
tellraw @a [{text:"\n"},{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Redstone Additions has been uninstalled.",color:"red"}]
tellraw @a [{text:"  "},{text:"You may now disable the datapack with: ",color:"gray"},{text:"/datapack disable \"file/redstone_additions\"",color:"yellow",click_event:{action:"suggest_command",command:"/datapack disable \"file/redstone_additions\""},hover_event:{action:"show_text",value:"Click to run"}}]
