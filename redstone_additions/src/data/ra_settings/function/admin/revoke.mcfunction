# /ra_settings:admin/revoke
# Take server-settings access off the nearest player.
#
# The counterpart to grant, and the reason the tag being permanent is safe to
# offer: access is removable, deliberately, by somebody who already has
# permission level 2.
#
# The trigger is reset as well as the tag. Enabling is per-player state that
# outlives the tag, and while the dispatcher refuses an untagged player anyway,
# leaving a trigger armed that can never do anything is untidy.

tag @p remove ra.admin
scoreboard players reset @p ra.settings.admin
tellraw @p [{text:"[Settings] ",color:"gold"},{text:"Server settings access removed. Your own preferences still work.",color:"gray"}]
