package ra.modules.wires

import ra.core.GameplayModule
import ra.core.Ns
import ra.core.PackPart
import ra.core.Ra
import ra.modules.wires.blocks.Placement
import ra.modules.wires.blocks.battery.Battery
import ra.modules.wires.blocks.boiler.Boiler
import ra.modules.wires.blocks.creative_eu.CreativeEu
import ra.modules.wires.blocks.creative_fluid.CreativeFluid
import ra.modules.wires.blocks.electric_breaker.ElectricBreaker
import ra.modules.wires.blocks.electric_consumer.ElectricConsumer
import ra.modules.wires.blocks.electric_furnace.ElectricFurnace
import ra.modules.wires.blocks.electric_furnace.ElectricFurnaceSettings
import ra.modules.wires.blocks.electric_furnace.ElectricFurnaceSmelting
import ra.modules.wires.blocks.electric_generator.ElectricGenerator
import ra.modules.wires.blocks.electric_switch.ElectricSwitch
import ra.modules.wires.blocks.electric_wire.ElectricWire
import ra.modules.wires.blocks.gas_pump.GasPump
import ra.modules.wires.blocks.gas_tank.GasTank
import ra.modules.wires.blocks.gas_valve.GasValve
import ra.modules.wires.blocks.industrial_light.IndustrialLight
import ra.modules.wires.blocks.liquid_drain.LiquidDrain
import ra.modules.wires.blocks.liquid_filter.LiquidFilter
import ra.modules.wires.blocks.liquid_pipe.LiquidPipe
import ra.modules.wires.blocks.liquid_pump.LiquidPump
import ra.modules.wires.blocks.liquid_tank.LiquidTank
import ra.modules.wires.blocks.liquid_valve.LiquidValve
import ra.modules.wires.blocks.network_gauge.NetworkGauge
import ra.modules.wires.blocks.solar_panel.SolarPanel
import ra.modules.wires.blocks.spout.Spout
import ra.modules.wires.blocks.tesla_coil.TeslaCoil
import ra.modules.wires.blocks.usage_meter.UsageMeter
import ra.modules.wires.bridge.Bridge
import ra.modules.wires.common.Common
import ra.modules.wires.debug.Debug
import ra.modules.wires.electric.Electric
import ra.modules.wires.electric.ElectricBreak
import ra.modules.wires.electric.EuApi
import ra.modules.wires.electric.Fuel
import ra.modules.wires.electric.Generator
import ra.modules.wires.fluid.Drain
import ra.modules.wires.fluid.DrainExperience
import ra.modules.wires.fluid.DrainPour
import ra.modules.wires.fluid.DrainItems
import ra.modules.wires.fluid.DrainPotion
import ra.modules.wires.fluid.Fluid
import ra.modules.wires.fluid.FluidBoiler
import ra.modules.wires.fluid.Pump
import ra.modules.wires.fluid.Source
import ra.modules.wires.liquid.Liquid
import ra.modules.wires.liquid.LiquidBreak
import ra.modules.wires.media.Media
import ra.modules.wires.machine.MachineClock
import ra.modules.wires.readout.Readout

/**
 * Transport Networks (`ra_wires`): fluid pipes and tanks, electric wires and machines, and the
 * bridges (valves, filters, breakers) that move contents between two networks without joining
 * them. The networks themselves are `ra_lib:transport`; this module is the blocks on top.
 *
 * Layout mirrors the generated paths: `blocks/<block>/` per block, `fluid/`, `electric/`,
 * `liquid/` (break handling), `bridge/`, `media/`, `common/` (display models), `debug/`,
 * `readout/` (the slow clock of the gauge and meter), `machine/` (the clock of the Tesla Coil and
 * Spout).
 * Items, recipes and the give bundle are one data table in [WiresBlocks].
 */
object WiresModule : GameplayModule {
	override val ns = Ns.WIRES
	override val title = "Transport Networks"

	/** Every object that writes `ra_wires` functions, one per Kotlin file. */
	private val parts: List<PackPart> = listOf(
		WiresLifecycle, Placement, Common, Media, Debug,
		Bridge, Liquid, LiquidBreak,
		Fluid, FluidBoiler, Pump, Drain, DrainItems, DrainExperience, DrainPotion, DrainPour, Source,
		Electric, ElectricBreak, EuApi, Fuel, Generator,
		Battery, Boiler, CreativeEu, CreativeFluid, ElectricBreaker, ElectricConsumer,
		ElectricFurnace, ElectricFurnaceSettings, ElectricFurnaceSmelting, ElectricGenerator,
		ElectricSwitch, ElectricWire, GasPump, GasTank, GasValve, IndustrialLight, LiquidDrain,
		LiquidFilter, LiquidPipe, LiquidPump, LiquidTank, LiquidValve, SolarPanel,
		Readout, NetworkGauge, UsageMeter,
		MachineClock, TeslaCoil, Spout,
	)

	override fun Ra.register() {
		with(WiresBlocks) { register() }
		for (part in parts) with(part) { register() }
	}
}
