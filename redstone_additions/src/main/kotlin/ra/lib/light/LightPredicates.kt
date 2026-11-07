package ra.lib.light

import io.github.ayfri.kore.arguments.numbers.ranges.rangeOrIntStart
import io.github.ayfri.kore.generated.arguments.types.PredicateArgument
import io.github.ayfri.kore.features.predicates.conditions.locationCheck
import io.github.ayfri.kore.features.predicates.predicate
import io.github.ayfri.kore.features.predicates.sub.light
import ra.core.Ns
import ra.core.Ra

/**
 * `ra_lib:light/at_least_<n>` for n in 1..15: "is the light here at least n". There is no command
 * that reads a light level, only a location predicate that tests one, so ra_lib:skin/light_here
 * binary-searches the level with these.
 */
object LightPredicates {
	fun atLeast(level: Int): PredicateArgument = PredicateArgument("light/at_least_$level", Ns.LIB.id)

	fun Ra.registerLightPredicates() {
		for (level in 1..15) pack.predicate("light/at_least_$level") {
			namespace = Ns.LIB.id
			locationCheck { light(rangeOrIntStart(level)) }
		}
	}
}
