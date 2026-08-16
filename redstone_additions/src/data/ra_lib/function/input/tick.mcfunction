# /ra_lib:input/tick
# Tick active input sessions.

execute as @a[tag=ra.input.active] run function ra_lib:input/router/tick

# A session that just produced a result (state 2) gets a fresh consume window, so a
# caller that polls less often than every tick cannot lose the answer to an expiry
# that happens to land on the same tick. The window is capped, so an answer nobody
# ever consumes still gets collected instead of leaking the session forever.
execute as @a[tag=ra.input.active,scores={ra.input.state=2,ra.input.ttl=..99}] run scoreboard players set @s ra.input.ttl 100

execute as @a[tag=ra.input.active] run scoreboard players remove @s ra.input.ttl 1

# Waiting sessions report the expiry to the player; unconsumed results are dropped
# silently, since from the player's side the interaction already finished.
execute as @a[tag=ra.input.active,scores={ra.input.state=1,ra.input.ttl=..0}] run function ra_lib:input/session/timeout
execute as @a[tag=ra.input.active,scores={ra.input.state=2,ra.input.ttl=..0}] run function ra_lib:input/session/cleanup
