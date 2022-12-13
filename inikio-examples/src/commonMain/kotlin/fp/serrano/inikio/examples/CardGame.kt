package fp.serrano.inikio.examples

import fp.serrano.inikio.plugin.InitialStyleDSL
import kotlin.random.Random

sealed interface Card

// INITIAL-STYLE DSL
// -----------------

@InitialStyleDSL
sealed interface Attack<out A>

data class Done<out A>(val result: A): Attack<A>
data class FlipCoin<out A>(
  val next: (Outcome) -> Attack<A>
): Attack<A> {
  enum class Outcome {
    HEADS, TAILS
  }
}
data class Draw<out A>(
  val next: (Card?) -> Attack<A>
): Attack<A>

// utility functions for creating attacks

suspend fun <A> AttackBuilder<A>.drawNRecursive(n: Int): List<Card> = when {
  n <= 0 -> emptyList()
  else -> {
    val first: List<Card> = listOfNotNull(draw())
    first + drawNRecursive(n - 1)
  }
}

suspend fun <A> AttackBuilder<A>.drawNUtility(n: Int): List<Card> =
  buildList {
    repeat(n) {
      add(draw())
    }
  }.filterNotNull()

// EXECUTION OF A PROGRAM
// ----------------------

fun Random.nextFlipOutcome(): FlipCoin.Outcome =
  if (nextBoolean()) FlipCoin.Outcome.HEADS else FlipCoin.Outcome.TAILS

tailrec fun <A> Attack<A>.execute(log: List<String> = emptyList()): Pair<List<String>, A> =
  when(this) {
    is Done -> log to result
    is FlipCoin -> next(Random.nextFlipOutcome()).execute(log)
    is Draw -> next(null).execute(log + "draw")
  }

// EXAMPLES OF PROGRAMS
// --------------------

val ironTailR: Attack<Int> get() = attack { ironTailWorkerR() }

suspend fun <A> AttackBuilder<A>.ironTailWorkerR(): Int =
  when (flipCoin()) {
    FlipCoin.Outcome.HEADS -> 30 + ironTailWorkerR()
    FlipCoin.Outcome.TAILS -> 0
  }
