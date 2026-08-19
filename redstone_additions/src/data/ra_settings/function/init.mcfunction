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

# `open` is the way IN, so everybody always has it -- it is the one trigger that
# has to be there before anything has happened.
scoreboard players enable @a ra.settings.open

# `act` is only meaningful while a menu is on screen, so it is enabled while that
# is true and reset when it stops being true. An enabled trigger shows up in
# everyone's /trigger completion whether it can do anything or not, and a list of
# names that do nothing is worse than a shorter list.
scoreboard objectives add ra.settings.viewing dummy

# Server-settings buttons. The objective is only ever enabled for a player holding
# ra.admin, so a hand-typed /trigger from anyone else is refused by the game
# before it reaches the dispatcher.
scoreboard objectives add ra.settings.admin trigger

# ra.admin IS THE PERMISSION, AND IT PERSISTS
# It is deliberately not cleared here. A server owner tags the people who should
# have settings access -- with /tag, or by opening the panel once through
# ra_settings:admin/show -- and that holds across reloads, which is the whole
# point of tagging somebody.
#
# The consequence, stated plainly: this is a role, not a session. Somebody who is
# de-opped keeps settings access until the tag is taken off them. Granting it
# needs permission level 2 (both /tag and /function do), so nobody can give it to
# themselves -- but taking it back is a deliberate act. See ra_settings:admin/revoke.

# Which page a player is looking at, so a row click knows what it belongs to.
scoreboard objectives add ra.settings.page dummy

# Which SERVER settings page an operator is on, so an action can redraw it.
scoreboard objectives add ra.settings.apage dummy

# The ra_lib:input request this player opened FOR SETTINGS. Sessions are shared
# with every other tool that asks for typed input, so consuming one without
# checking whose it is takes the Data Handler's answer away from it.
scoreboard objectives add ra.settings.req dummy

# A row waiting on typed input, as row index + 1. Mirrors ra.dh.pending, and for
# the same reason: the answer arrives some ticks after the click, and something
# has to remember what the question was.
scoreboard objectives add ra.settings.pend dummy

# Scratch for the renderer and the dispatcher.
scoreboard objectives add ra.set.tmp dummy

# Remembers last tick's debug setting, so ra_settings:hooks can act on the change
# rather than on the value. Created here rather than there: hooks runs every tick,
# and re-adding an existing objective logs a complaint every one of them.
scoreboard objectives add ra.u.dbg.was dummy

# Rebuilds the menu registry and seeds any setting that has no value yet. Values
# already chosen are never overwritten -- see the generated ra_settings:defaults.
function #ra_settings:register
