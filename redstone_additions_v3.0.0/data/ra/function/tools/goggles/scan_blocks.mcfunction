# /ra:tools/goggles/scan_blocks
# Find nearby custom blocks and dispatch billboard creation
# Context: as player with goggles, at player position
# Max scan distance: 16 blocks
# Each module owns its own draw_display functions

# Delegate to per-module dispatchers
function ra_interactive:goggles/draw_displays
function ra_gates:goggles/draw_displays
function ra_sensors:goggles/draw_displays
function ra_wireless:goggles/draw_displays
function ra_wires:goggles/draw_displays
