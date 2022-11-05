package fp.serrano.inikio.util

import fp.serrano.inikio.ProgramBuilder

/**
 * Repeats the same program [f] a given amount of times [n].
 */
public suspend fun <Action, Result, A, T : ProgramBuilder<Action, Result>> T.repeat(
  n: Int, f: suspend T.() -> A
): List<A> = when {
  n <= 0 -> emptyList()
  else -> listOf(f()) + repeat(n - 1, f)
}

/**
 * Record the execution of [f] only if the [condition] is `true`.
 */
public suspend fun <Action, Result, T : ProgramBuilder<Action, Result>> T.`when`(
  condition: Boolean,
  f: suspend T.() -> Unit
): Unit = if (condition) f() else Unit

/**
 * Represents the infinite execution of [f].
 *
 * Usually your DSL has some kind of escape mechanism if you are using this function.
 */
public suspend fun <Action, Result, A, B, T : ProgramBuilder<Action, Result>> T.forever(
  f: suspend T.() -> A
): B { f() ; return forever(f) }

/**
 * Execute the program [f] until it returns `false`. The program is executed at least once.
 */
public suspend fun <Action, Result, T : ProgramBuilder<Action, Result>> T.`while`(
  f: suspend T.() -> Boolean
): Unit = if (f()) `while`(f) else Unit