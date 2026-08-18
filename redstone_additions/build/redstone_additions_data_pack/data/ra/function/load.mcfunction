# /data/ra/function/load.mcfunction
# Redstone Additions v5.1.13 - Core Load
# Initializes all scoreboards, libraries, and sub-modules

# ========================== SCOREBOARDS ==========================
# Initialize scoreboards
# Data handler scoreboards
scoreboard objectives add ra.edit_type trigger
scoreboard objectives add ra.dh.action trigger
scoreboard objectives add ra.dh.pending dummy
# Clipboard slot number, assigned on first use. See ra:tools/clipboard/ensure_id.
scoreboard objectives add ra.clip.id dummy
# Debug-only input handler objective (commented by request)
# scoreboard objectives add ra.input_handler.action trigger
scoreboard players enable @a ra.edit_type
scoreboard players enable @a ra.dh.action
# Debug-only input handler objective enable (commented by request)
# scoreboard players enable @a ra.input_handler.action

# Initialize shared temp storage used by macros and status rendering
data merge storage ra:temp {has_facing:0b,facing_name:"north",block_id:"",io:{},status_literal:{},billboard:{},goggles_props:{},upgrade:{}}

# Initialize configurable text-display offsets once.
function ra:tools/goggles/billboard/init_offsets

# ========================== SUB-LOAD ==
# Initialize library systems
function ra_lib:init

# One-off sweep for worlds built before the redstone library stopped producing
# per-source tags. See ra_lib:redstone/clear. Harmless on a fresh world, and on an
# upgraded one it costs a handful of commands per loaded custom block, once.
execute as @e[type=marker,tag=ra.custom_block] run function ra_lib:redstone/clear

# Initialize sub-modules
function ra_interactive:load
function ra_storage:load
function ra_sensors:load
function ra_gates:load
function ra_wireless:load
function ra_wires:load
function ra_chunk_loader:load
function ra_multiblock:load
function ra_enchanting:load
function ra_infinite:load
function ra_jetpacks:load
function ra_ender:load

# Initialize multiblock library
function ra_lib_multiblock:init

# ========================== TICKLOOP ==========================
# Schedule tick for the next game tick to avoid load-time command bursts
# Bring a world saved by an older version up to date. See ra_migrations:run.
# The wrench menu: which row was clicked, and which block it belongs to.
scoreboard objectives add ra.wrench trigger
scoreboard players enable @a ra.wrench
scoreboard objectives add ra.wr.x dummy
scoreboard objectives add ra.wr.y dummy
scoreboard objectives add ra.wr.z dummy

function ra:tools/wrench/init_registry
function ra:tools/readonly/init_registry

function ra_migrations:run

schedule function ra:tick 1t

# ========================== WELCOME MESSAGE ==========================
# Load message_block
tellraw @a [{text:"[RA_Lib] ",color:"gold"},{text:"v5.1.13 loaded",color:"green"}]

# Welcome message_block
tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Redstone Additions v5.1.13 loaded!",color:"green"}]
tellraw @a [{text:"Use ",color:"gray"},{text:"/function ra:give_all_items",color:"yellow",hover_event:{action:"show_text",value:"Give all items"},click_event:{action:"suggest_command",command:"/function ra:give_all_items"}},{text:" to get items",color:"gray"}]
