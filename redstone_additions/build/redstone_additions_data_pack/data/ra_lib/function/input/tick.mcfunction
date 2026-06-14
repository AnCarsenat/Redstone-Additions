# /ra_lib:input/tick
# Tick active input sessions.

execute as @a[tag=ra.input.active] run function ra_lib:input/router/tick
execute as @a[tag=ra.input.active] run scoreboard players remove @s ra.input.ttl 1
execute as @a[tag=ra.input.active,scores={ra.input.ttl=..0}] run function ra_lib:input/session/timeout