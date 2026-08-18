# /ra_wires:common/tick_cleanup
# Clear transient per-tick transport tags.
#
# There are none left. The electric system's ra.wires.eu_src latch went with the
# push model: charge lives on the network now, so no node is ever mid-handover to
# another and there is no per-tick state to unwind. Kept as a no-op hook so
# ra_wires:tick keeps a place to put one if a future system needs it.
