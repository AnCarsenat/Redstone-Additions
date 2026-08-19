# /ra_settings:admin_expire
# End a server-settings session that has gone quiet. Context: as the player.

tag @s remove ra.admin
scoreboard players reset @s ra.settings.admin
tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Server settings session expired. Reopen it with ",color:"gray"},{text:"/function ra_settings:admin/show",color:"yellow",click_event:{action:"suggest_command",command:"/function ra_settings:admin/show"}}]
