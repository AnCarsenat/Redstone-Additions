# /ra_jetpacks:tick
# Drive every worn jetpack
#
# Every dispatch here is `as @s at @s`. `as` alone changes who runs the command
# but not where it runs, so the particles and sounds inside these functions were
# being played at the command origin instead of at the player, and nobody ever
# saw or heard them.

# Kit right-click bookkeeping: a player who stopped clicking loses the held tag,
# so the next click counts as a new one.
tag @a[tag=ra.jp.kit_active,tag=!ra.jp.kit_clicked] remove ra.jp.kit_active
tag @a[tag=ra.jp.kit_clicked] remove ra.jp.kit_clicked

# Mode switches requested with /trigger ra.jp.mode.
execute as @a[scores={ra.jp.mode=1..}] at @s run function ra_jetpacks:mode/toggle

# Engine sound muted or unmuted with /trigger ra.jp.sound.
execute as @a[scores={ra.jp.sound=1..}] at @s run function ra_jetpacks:mode/sound_toggle

# Whole jetpack switched off or on with /trigger ra.jp.power.
execute as @a[scores={ra.jp.power=1..}] at @s run function ra_jetpacks:mode/power_toggle

# Upgrade menu, opened and driven with /trigger ra.jp.kits.
execute as @a[scores={ra.jp.kits=1..}] at @s run function ra_jetpacks:kit/menu_action

# Anyone wearing a jetpack chestplate.
execute as @a at @s if items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack:1b}}] run function ra_jetpacks:flight/tick_player

# Anyone who was wearing one a moment ago and is not now: put gravity back.
execute as @a[tag=ra.jetpack_on] at @s unless items entity @s armor.chest *[minecraft:custom_data~{ra:{jetpack:1b}}] run function ra_jetpacks:flight/stop
