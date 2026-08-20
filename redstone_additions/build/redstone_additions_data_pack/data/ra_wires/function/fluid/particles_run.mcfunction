# /ra_wires:fluid/particles_run {particle:"..."}
# Internal: the medium's own particle, so water splashes, lava sparks and powder
# snow puffs snowflakes rather than everything emitting the same grey smoke.

$particle $(particle) ~ ~0.6 ~ 0.25 0.25 0.25 0.02 8 normal @a[scores={ra.u.par=1..}]
