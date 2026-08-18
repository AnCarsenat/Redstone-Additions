# /ra_migrations:run
# Every migration this pack has ever needed, oldest first.
# Called from ra:load, before anything else touches the world.
#
# ONE FILE PER VERSION STEP, NAMED FOR THE STEP
# A migration is tied to the two versions it bridges, so the file says which
# they are: 5.1.7-to-5.1.9 turns a 5.1.7 world into a 5.1.9 one. They run in
# order and they all run every load, which means every one of them has to be
# safe to run twice — none may destroy or overwrite state, only fill in what a
# newer version expects and an older one never wrote.
#
# These names are IDENTIFIERS, not the pack version -- a find-and-replace that
# bumps the version across the repo must not touch them, or the chain silently
# renames itself to nonsense like 5.1.9-to-5.1.9.
#
# The name is `-to-` rather than `->` because a resource location path may only
# contain [a-z0-9_.-/]. A file with `>` in its name is skipped by the datapack
# loader, and a migration that is silently never loaded is worse than one that
# is named slightly differently.

function ra_migrations:5.1.7-to-5.1.8
function ra_migrations:5.1.8-to-5.1.9
function ra_migrations:5.1.9-to-5.1.10
function ra_migrations:5.1.11-to-5.1.12
