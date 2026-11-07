package ra.tools.goggles.status

import ra.api.Goggles
import ra.core.Ra
import ra.core.callWith
import ra.core.define
import ra.core.raw

/** Multiblock-specific status lines (heat, enabled). As the multiblock marker, at its base. */
internal fun Ra.registerGogglesStatus() = pack.define(Goggles.Status.readStatusMultiblock) {
	// Heat is scoreboard-backed for the blast forge and updates live.
	raw("""data modify storage ra:temp status_literal set value {y:2.35,label:"Heat: ",value:"N/A",value_color:"gold",suffix:"/1000"}""")
	raw("execute if score @s ra.heat matches -2147483648..2147483647 store result storage ra:temp status_literal.value int 1 run scoreboard players get @s ra.heat")
	callWith(Goggles.Billboard.showLiteralLine, "storage ra:temp status_literal")

	// Enabled remains NBT-backed, from the multiblock's properties.
	raw("""data modify storage ra:temp status_literal set value {y:2.1,label:"Enabled: ",value:"N/A",value_color:"aqua",suffix:""}""")
	raw("execute if data entity @s data.properties.enabled run data modify storage ra:temp status_literal.value set from entity @s data.properties.enabled")
	callWith(Goggles.Billboard.showLiteralLine, "storage ra:temp status_literal")
}
