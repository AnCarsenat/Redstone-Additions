package ra.multiblocklib

import io.github.ayfri.kore.arguments.types.literals.allEntities
import io.github.ayfri.kore.arguments.types.literals.self
import io.github.ayfri.kore.commands.execute.execute
import io.github.ayfri.kore.commands.function
import io.github.ayfri.kore.commands.kill
import io.github.ayfri.kore.commands.scoreboard.scoreboard
import io.github.ayfri.kore.generated.EntityTypes
import ra.api.MultiblockLib
import ra.core.Ns
import ra.core.Obj
import ra.core.Ra
import ra.core.Tag
import ra.core.define
import ra.core.raw
import ra.core.ref

/** Periodic validation of standing multiblocks, and disassembly of broken ones. */
internal object Validation {
	val validateSingle = ref(Ns.LIB_MULTIBLOCK, "validate_single")

	fun Ra.registerValidation() {
		// Called periodically; disassembles any multiblock whose structure is broken.
		pack.define(MultiblockLib.validateAll) {
			execute {
				asTarget(allEntities { type = EntityTypes.MARKER; tag = Tag.MULTIBLOCK })
				at(self())
				run { function(validateSingle) }
			}
		}

		// As the multiblock marker, at its position.
		pack.define(validateSingle) {
			// Grace period: skip validation for newly created markers, and drop the tag for next time.
			raw("execute if entity @s[tag=${Assembly.GRACE}] run return run tag @s remove ${Assembly.GRACE}")

			scoreboard.players.set(self(), Obj.MULTIBLOCK, 0)

			// Type-specific structure check via registered hooks (sets the score to 1 if valid).
			function(hookTag(MultiblockLib.Hooks.CHECK_STRUCTURE))

			raw("""execute if entity @a[tag=${Tag.DEBUG},limit=1] if score @s ${Obj.MULTIBLOCK} matches 0 at @s run tellraw @a[tag=${Tag.DEBUG}] [{text:"[MB Debug] ",color:"gold"},{text:"FAIL ",color:"red"},{text:"type="},{nbt:"data.type",entity:"@s",color:"aqua"},{text:" facing="},{nbt:"data.facing",entity:"@s",color:"aqua"},{text:" pos="},{nbt:"Pos",entity:"@s",color:"yellow"}]""")
			raw("""execute if entity @a[tag=${Tag.DEBUG},limit=1] if score @s ${Obj.MULTIBLOCK} matches 1 at @s run tellraw @a[tag=${Tag.DEBUG}] [{text:"[MB Debug] ",color:"gold"},{text:"OK ",color:"green"},{text:"type="},{nbt:"data.type",entity:"@s",color:"aqua"},{text:" facing="},{nbt:"data.facing",entity:"@s",color:"aqua"}]""")

			execute {
				ifCondition { score(self(), Obj.MULTIBLOCK) equalTo 0 }
				run { function(MultiblockLib.disassemble) }
			}
		}

		// As the multiblock marker.
		pack.define(MultiblockLib.disassemble) {
			// Type-specific cleanup via registered hooks.
			function(hookTag(MultiblockLib.Hooks.ON_BREAK))

			raw("execute at @s run playsound minecraft:block.anvil.land block @a[distance=..16] ~ ~ ~ 0.5 0.8")
			raw("execute at @s run particle minecraft:smoke ~ ~0.5 ~ 0.3 0.3 0.3 0.02 15")
			raw("""tellraw @a[distance=..16] [{text:"[RA] ",color:"gold"},{text:"Multiblock disassembled.",color:"yellow"}]""")

			kill(self())
		}
	}
}
