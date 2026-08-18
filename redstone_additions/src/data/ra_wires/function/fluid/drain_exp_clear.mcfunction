# /ra_wires:fluid/drain_exp_clear
# Internal: drop the working tag, on every exit path out of drain_exp.
# Returns 0 so it can be used as `return run function ...`.

tag @a[tag=ra.wires.xp_giver] remove ra.wires.xp_giver
return 0
