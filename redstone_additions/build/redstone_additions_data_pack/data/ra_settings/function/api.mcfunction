# /ra_settings:api
# Not runnable. How a module gets settings.
#
# Add tools/settings/<page>.json. Nothing else. tools/settings_gen.py turns it
# into the menu registry, the defaults, and the operator function tree at build
# time, so there is no list anywhere that has to be kept in step with it.
#
#   {"id":"wires","title":"Power & Fluids","namespace":"ra_wires","rows":[ ... ]}
#
# ROW TYPES
#
#   {"type":"bool","key":"...","label":"...","scope":"global"|"user","default":0|1}
#   {"type":"int","key":"...","label":"...","scope":"global"|"user",
#    "default":N,"min":N,"max":N,"step":N}
#   {"type":"list","key":"...","label":"...","scope":"global"|"user",
#    "default":0,"values":["a","b"]}
#       Stores the INDEX, not the string, so renaming a choice moves the label
#       without orphaning what anybody had selected.
#   {"type":"block","block":"electric_furnace","label":"Electric Furnace"}
#       Whether the block may be placed. Disabling never touches blocks already
#       standing in the world.
#   {"type":"prop","block":"electric_generator","prop":"generation_rate",
#    "label":"Generator EU/tick","default":60,"min":1,"max":10000,"step":10}
#       The default for a per-block property, written onto the marker when it is
#       placed -- see ra_settings:placement/seed for why it is applied there and
#       not inside ra_lib:util/property.
#
# SCOPE DECIDES HOW IT IS REACHED, NOT JUST WHO MAY CHANGE IT
#   global -- generates /function ra_settings:admin/<page>/<label>/<action>.
#             Needs permission level 2, and autocompletes, which is the only way
#             an operator discovers a setting without being told its name.
#             Does NOT appear in the in-game menu at all.
#   user   -- appears in the /trigger menu and nowhere else. Needs
#             "obj":"ra.u.snd", a short stable objective name: a player's saved
#             choice is found by that name, so renaming one resets everybody.
#             Keep it under 16 characters.
#
# READING A SETTING
#   function ra_settings:get  {key:"..."}                        -> #setting ra.set.tmp
#   function ra_settings:prop {block:"...",prop:"...",default:N} -> #setting ra.set.tmp
#   function ra_settings:user {obj:"...",default:N}              -> #setting ra.set.tmp  (as the player)
#   function ra_settings:enabled {block:"..."}                   -> 1 when placeable
