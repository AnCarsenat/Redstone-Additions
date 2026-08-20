# /ra_settings:server_open
# Try to open the server settings, and if that is not this player's to do, put the
# command where they can read it. Context: as the player.
#
# WHY THIS IS NOT JUST A LINK TO THE FUNCTION
# A button that runs /function raises a confirmation dialog, and for a player
# without permission it then fails anyway. A button that runs a trigger does
# neither -- but a trigger handler is datapack code, which runs at permission
# level 2, so it COULD open the panel for anybody. That would hand server
# settings to every player on the server, which is the one thing this must not do.
#
# So the handler opens the panel only for a player who already holds ra.admin,
# meaning they have opened it through the real function before and the game
# already checked them. Everyone else is given the command as a SUGGESTION: it
# lands in the chat box unrun, and pressing enter puts the permission check back
# where it belongs -- on the game, not on this pack's guess about who is allowed.

execute if entity @s[tag=ra.admin] run return run function ra_settings:admin/show

tellraw @s [{text:"[Settings] ",color:"gold"},{text:"Server settings need operator permission.",color:"gray"}]
tellraw @s [{text:"  "},{text:"/function ra_settings:admin/show",color:"yellow",bold:true,hover_event:{action:"show_text",value:"Puts this in your chat box — press enter to run it. It will only work if you are an operator."},click_event:{action:"suggest_command",command:"/function ra_settings:admin/show"}}]
