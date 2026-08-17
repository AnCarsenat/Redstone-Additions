# /ra_lib_multiblock:structure/check_one {pos:"~x ~y ~z",match:"..."}
# Internal: clear #mb_ok when one required block is absent.
# `match` is substituted verbatim, so ids, #block_tags and block states all work.

$execute unless block $(pos) $(match) run scoreboard players set #mb_ok ra.multiblock 0
