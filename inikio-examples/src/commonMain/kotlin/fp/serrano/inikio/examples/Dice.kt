package fp.serrano.inikio.examples

import fp.serrano.inikio.FixedResultType
import fp.serrano.inikio.InitialStyleDSL

@InitialStyleDSL
@FixedResultType("kotlin.Int")
sealed interface Dice
data class Result(val result: Int): Dice
data class Throw(val next: (Int) -> Dice): Dice