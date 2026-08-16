# /ra_wires:common/tick_cleanup
# Clear transient per-tick transport tags.
#
# The fluid side no longer uses any: contents live on the network, so there is no
# per-tick source marking or move latch left to clear. Only the electric system,
# which still pushes node to node, needs this.

tag @e[type=marker,tag=ra.wires.eu_src] remove ra.wires.eu_src
