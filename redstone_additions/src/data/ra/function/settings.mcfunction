# /ra:settings
# Open the server settings. The short way in.
#
# ra_settings:admin/show is the real entry point and does the work; this exists
# because that path is four segments deep and an operator wanting the panel
# should not have to tab through the tree to reach its front door. `ra:settings`
# is two tokens and sorts next to the pack's other top-level functions.

function ra_settings:admin/show
