# /data/ra/function/load.mcfunction
# Redstone Additions v5.1.15 - Core Load
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

# Settings. After the modules have loaded, because each module registers its own
# page through #ra_settings:register and cannot do that before it exists.
function ra_settings:init

function ra:tools/wrench/init_registry
function ra:tools/readonly/init_registry

function ra_migrations:run

schedule function ra:tick 1t

# ========================== WELCOME MESSAGE ==========================
# Gated on a setting, because a server that has run this pack for a year does not
# need to tell everyone about it on every reload. Read into a score first: the
# messages below are plain tellraw lines and `execute if data` on each would be
# the same test written six times.
execute store result score #welcome ra.temp run function ra_settings:get {key:"welcome",default:1}
execute if score #welcome ra.temp matches ..0 run return 0
# Load message_block
tellraw @a [{text:"[RA_Lib] ",color:"gold"},{text:"v5.1.15 loaded",color:"green"}]

# Welcome message_block
tellraw @a [{text:"[",color:"dark_gray"},{text:"RA",color:"gold",bold:true},{text:"] ",color:"dark_gray"},{text:"Redstone Additions v5.1.15 loaded!",color:"green"}]
# The server button fires a TRIGGER, not the function. A /function link raises a
# confirmation dialog and then fails for anyone without permission; the trigger
# opens the panel for an operator who already has a session, and hands everyone
# else the command unrun so the game can refuse them properly.
# See ra_settings:server_open.
tellraw @a [{text:"Server settings: ",color:"gray"},{text:"[ Open ]",color:"yellow",bold:true,hover_event:{action:"show_text",value:"World-wide settings — needs permission level 2"},click_event:{action:"run_command",command:"/trigger ra.settings.open set 2"}}]

# User settings are SUGGESTED rather than run, so the command lands in the chat
# box and the player reads it before pressing enter. This one has to be
# remembered -- it is the only way back into the menu once this message has
# scrolled away -- and a button that silently works teaches nobody its name.
tellraw @a [{text:"Your settings: ",color:"gray"},{text:"/trigger ra.settings.open",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Click to put this in your chat box — then press enter"},click_event:{action:"suggest_command",command:"/trigger ra.settings.open"}}]
tellraw @a [{text:"Use ",color:"gray"},{text:"/function ra:give_all_items",color:"yellow",hover_event:{action:"show_text",value:"Give all items"},click_event:{action:"suggest_command",command:"/function ra:give_all_items"}},{text:" to get items",color:"gray"}]
