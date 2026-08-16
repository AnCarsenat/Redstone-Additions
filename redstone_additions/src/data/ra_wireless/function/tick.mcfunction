# /data/ra_wireless/function/tick.mcfunction
# Tick all wireless redstone blocks — called every game tick from ra:tick

# Tick emitters and receivers
function ra_wireless:blocks/emitter/tick
function ra_wireless:blocks/receiver/tick

# Finish channel entry for anyone who has a book request outstanding.
execute as @a[scores={ra.remote.pending=1..}] run function ra_wireless:tools/remote/apply_pending
