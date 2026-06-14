# /data/ra/function/tick.mcfunction
# Main tick loop — runs every game tick
# Handles tool cleanup, placement detection, and dispatches all module ticks

# Remove active tool tags for players who stopped clicking
# Tags get re-added each tick while clicking via advancement
# If tag exists but wasn't refreshed this tick, player stopped
tag @a[tag=ra.cdh_active,tag=!ra.cdh_clicked] remove ra.cdh_active
tag @a[tag=ra.dh_active,tag=!ra.dh_clicked] remove ra.dh_active
tag @a[tag=ra.wrench_active,tag=!ra.wrench_clicked] remove ra.wrench_active
tag @a[tag=ra.remote_active,tag=!ra.remote_clicked] remove ra.remote_active
# Debug-only input handler active tag cleanup (commented by request)
# tag @a[tag=ra.input_handler_active,tag=!ra.input_handler_clicked] remove ra.input_handler_active
tag @a[tag=ra.cdh_clicked] remove ra.cdh_clicked
tag @a[tag=ra.dh_clicked] remove ra.dh_clicked
tag @a[tag=ra.wrench_clicked] remove ra.wrench_clicked
tag @a[tag=ra.remote_clicked] remove ra.remote_clicked
# Debug-only input handler clicked tag cleanup (commented by request)
# tag @a[tag=ra.input_handler_clicked] remove ra.input_handler_clicked

# Tick modular input sessions
function ra_lib:input/tick

# Tick non-OP input data handler actions
function ra:tools/data_handler/tick

# Debug-only input handler tick (commented by request)
# function ra:tools/input_data_handler/tick

# Placement system - detect bats for custom block placement
function ra:placement/detect_bats

# Keep all custom block markers centered on world-grid coordinates.
execute as @e[type=marker,tag=ra.custom_block] at @s align xyz positioned ~0.5 ~0.5 ~0.5 run tp @s ~ ~ ~

# Run interactive blocks tick
function ra_interactive:tick

# Run storage blocks tick
function ra_storage:tick

# Run sensor blocks tick
function ra_sensors:tick

# Run logic gates tick
function ra_gates:tick

# Run wireless redstone tick
function ra_wireless:tick

# Run liquids, gases, and electric wires tick
function ra_wires:tick

# Run chunk loader tick
function ra_chunk_loader:tick

# Run multiblock tick
function ra_multiblock:tick

# Run goggles scan
function ra:tools/goggles/tick

schedule function ra:tick 1t