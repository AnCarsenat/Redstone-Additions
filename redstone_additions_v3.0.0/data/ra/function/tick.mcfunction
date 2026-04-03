# /data/ra/function/tick.mcfunction
# Main tick loop — runs every game tick
# Handles tool cleanup, placement detection, and dispatches all module ticks

# Remove active tool tags for players who stopped clicking
# Tags get re-added each tick while clicking via advancement
# If tag exists but wasn't refreshed this tick, player stopped
tag @a[tag=ra.cdh_active,tag=!ra.cdh_clicked] remove ra.cdh_active
tag @a[tag=ra.wrench_active,tag=!ra.wrench_clicked] remove ra.wrench_active
tag @a[tag=ra.remote_active,tag=!ra.remote_clicked] remove ra.remote_active
tag @a[tag=ra.cdh_clicked] remove ra.cdh_clicked
tag @a[tag=ra.wrench_clicked] remove ra.wrench_clicked
tag @a[tag=ra.remote_clicked] remove ra.remote_clicked

# Placement system - detect bats for custom block placement
function ra:placement/detect_bats

# Run interactive blocks tick
function ra_interactive:tick

# Run sensor blocks tick
function ra_sensors:tick

# Run logic gates tick
function ra_gates:tick

# Run wireless redstone tick
function ra_wireless:tick

# Run example multiblocks tick
function ra_example:tick

# Run chunk loader tick
function ra_chunk_loader:tick

# Run multiblock tick
function ra_multiblock:tick

# Run goggles scan
function ra:tools/goggles/tick

schedule function ra:tick 1t