# /ra_wireless:tools/remote/apply_hotbar {slot:N}
# Internal: stamp the channel onto a remote sitting in hotbar slot N.

$item modify entity @s hotbar.$(slot) ra_wireless:set_channel
