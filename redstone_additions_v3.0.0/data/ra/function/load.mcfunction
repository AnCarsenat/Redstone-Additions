# /data/ra/function/load.mcfunction
# Redstone Additions v4.0.1 - Core Load
# Initializes all scoreboards, libraries, and sub-modules

# ========================== SCOREBOARDS ==========================
# Initialize scoreboards
# Data handler scoreboards
scoreboard objectives add ra.edit_type trigger
scoreboard objectives add ra.edit_step dummy
scoreboard objectives add ra.dh.action trigger
scoreboard objectives add ra.dh.pending dummy
# Debug-only input handler objective (commented by request)
# scoreboard objectives add ra.input_handler.action trigger
scoreboard players enable @a ra.edit_type
scoreboard players enable @a ra.dh.action
# Debug-only input handler objective enable (commented by request)
# scoreboard players enable @a ra.input_handler.action

# Initialize shared temp storage used by macros and status rendering
data merge storage ra:temp {has_facing:0b,facing_name:"north",block_id:"",io:{},status_literal:{},billboard:{},goggles_props:{},upgrade:{}}

# ========================== SUB-LOAD ==
# Initialize library systems
function ra_lib:init

# Initialize sub-modules
function ra_interactive:load
function ra_sensors:load
function ra_gates:load
function ra_wireless:load
function ra_chunk_loader:load
function ra_multiblock:load

# Initialize multiblock library
function ra_lib_multiblock:init

# ========================== TICKLOOP ==========================
# Run tick
function ra:tick

# ========================== WELCOME MESSAGE ==========================
# Load message
tellraw @a [{text:"[RA_Lib] ",color:"gold"},{text:"v4.0.1 loaded",color:"green"}]

# Welcome message
tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Redstone Additions v4.0.1 loaded!",color:"green"}]
tellraw @a [{text:"Use ",color:"gray"},{text:"/function ra:give_all_items",color:"yellow",hover_event:{action:"show_text",value:"Give all items"},click_event:{action:"suggest_command",command:"/function ra:give_all_items"}},{text:" to get items",color:"gray"}]
