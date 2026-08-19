# /ra_settings:init
# Bring the settings system up. Called once from ra:load.
#
# WHAT THIS IS
# A place for values that are not a property of one placed block. Per-block
# properties already have a home -- the marker's data.properties, edited with the
# wrench or the Data Handler. What had nowhere to live was the other two kinds:
# pack-wide balance an admin wants to tune once, and per-player preferences that
# should differ between two people standing in the same room.
#
# THE TWO SCOPES, AND WHY THEY ARE REACHED DIFFERENTLY
#   global -- one value for the world, in storage ra:settings global. Changed
#             ONLY through /function ra_settings:admin/..., which requires
#             permission level 2. Being a function tree is also what makes it
#             discoverable: /function autocompletes, so an operator can tab
#             through every setting without knowing any of their names in
#             advance. A macro argument would complete to nothing.
#   user   -- one value per player. Changed from the in-game menu with /trigger,
#             which needs no permissions.
#
# The split is not about taste. Anything that alters game balance is global;
# anything that alters what one person sees or hears is user. A per-player EU
# cost would let one player mine cheaper than someone standing beside them.
#
# EVERYTHING BELOW THE TAG IS GENERATED
# tools/settings_gen.py reads tools/settings/*.json and emits ra_settings:pages,
# ra_settings:defaults and the whole admin tree. Add a page by adding a JSON file
# -- there is no list here to keep in step with it.

# The objective a player actually types. Named to be remembered rather than to be
# short: it is printed once at load as a suggestion and then has to survive in
# somebody's memory until they want it. `/trigger ra.settings.open` with no
# argument adds 1, which is exactly the "open the root menu" value, so there is
# no `set 1` for anyone to get wrong.
scoreboard objectives add ra.settings.open trigger
scoreboard objectives add ra.settings.act trigger
scoreboard players enable @a ra.settings.open
scoreboard players enable @a ra.settings.act

# Server-settings buttons, and the session that authorises them. The objective is
# only ever enabled for a player holding ra.admin, so a hand-typed /trigger from
# anyone else is refused by the game before it reaches the dispatcher.
scoreboard objectives add ra.settings.admin trigger
scoreboard objectives add ra.admin.ttl dummy

# A tag outlives the permission that granted it, so no session survives a reload.
tag @a remove ra.admin

# Which page a player is looking at, so a row click knows what it belongs to.
scoreboard objectives add ra.settings.page dummy

# A row waiting on typed input, as row index + 1. Mirrors ra.dh.pending, and for
# the same reason: the answer arrives some ticks after the click, and something
# has to remember what the question was.
scoreboard objectives add ra.settings.pend dummy

# Scratch for the renderer and the dispatcher.
scoreboard objectives add ra.set.tmp dummy

# Rebuilds the menu registry and seeds any setting that has no value yet. Values
# already chosen are never overwritten -- see the generated ra_settings:defaults.
function #ra_settings:register
