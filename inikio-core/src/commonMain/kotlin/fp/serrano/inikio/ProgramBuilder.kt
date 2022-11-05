package fp.serrano.inikio

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

/**
 * This class provides a DSL to build initial-style programs, given the type of basic actions or instructions.
 *
 * The type arguments are a bit tricky to use, due to the restrictions Kotlin's type system:
 * * To create generic utilities over any program, you should use a completely polymorphic
 *   `ProgramBuilder<Action, Result>` as the extension receiver.
 * * For each particular action you usually want to instantiate [ProgramBuilder] using `<Action<A>, A>`
 *   everywhere it appears. We highly recommend to create a `typealias` for it.
 */
@Suppress("UNCHECKED_CAST")
public open class ProgramBuilder<Action, Result> public constructor(
  private val endWith: (Result) -> Action,
  private val exceptional: (Throwable) -> Action = { e -> throw e }
) {
  public sealed interface State<Action, Result>
  private data class DoneState<Action, Result>(
    val result: Result
  ): State<Action, Result>
  private data class ExceptionState<Action, Result>(
    val result: Throwable
  ): State<Action, Result>
  private data class ActionState<Action, Result>(
    val f: ((Any?) -> Action) -> Action,
    val next: Continuation<Any?>
  ): State<Action, Result>

  private var current: State<Action, Result> = ExceptionState(IllegalStateException("empty trace"))

  /**
   * Record the execution of an action which consumes a value of type `R`.
   */
  public suspend fun <R> perform(f: ((R) -> Action) -> Action): R = suspendCoroutine { k ->
    current = ActionState(f, k as Continuation<Any?>)
  }

  /**
   * Record the execution of an action which doesn't consume any value.
   */
  public suspend fun performUnit(f: (() -> Action) -> Action): Unit =
    perform { arg -> f { arg(Unit) } }

  /**
   * Record the execution of an action which doesn't consume any value,
   * and requires an additional argument of type `A`.
   */
  public suspend fun <A> performUnit(f: (A, () -> Action) -> Action, x: A): Unit =
    perform { arg -> f(x) { arg(Unit) } }

  /**
   * Record the execution of an action which doesn't consume any value,
   * and requires additional arguments of types `A` and `B`.
   */
  public suspend fun <A, B> performUnit(f: (A, B, () -> Action) -> Action, x: A, y: B): Unit =
    perform { arg -> f(x, y) { arg(Unit) } }

  /**
   * Record the execution of an action which consumes a value of type `R`,
   * and requires an additional argument of type `A`.
   */
  public suspend fun <A, R> perform(f: (A, (R) -> Action) -> Action, x: A): R =
    perform { arg -> f(x, arg) }

  /**
   * Record the execution of an action which consumes a value of type `R`,
   * and requires additional arguments of types `A` and `B`.
   */
  public suspend fun <A, B, R> perform(f: (A, B, (R) -> Action) -> Action, x: A, y: B): R =
    perform { arg -> f(x, y, arg) }

  internal fun done(result: Result) {
    current = DoneState(result)
  }

  internal fun exception(e: Throwable) {
    current = ExceptionState(e)
  }

  internal fun execute(): Action = when (val c = current) {
    is DoneState -> endWith(c.result)
    is ExceptionState -> exceptional(c.result)
    is ActionState<Action, Result> -> c.f { x: Any? ->
      c.next.resume(x)
      execute()
    }
  }
}

/**
 * Turns the DSL provided by [ProgramBuilder] into an actual [Action].
 * This version is used for "pure" programs with no exceptional cases.
 *
 * @param endWith Reference to the "final" instruction in [Action].
 *
 * @exception Throwable If an exception is raised during the execution of [f], it will be propagated.
 */
public fun <Action, Result> program(
  endWith: (Result) -> Action,
  f: suspend ProgramBuilder<Action, Result>.() -> Result
): Action = program(endWith, { e -> throw e }, f)

/**
 * Turns the DSL provided by [ProgramBuilder] into an actual [Action].
 *
 * @param endWith Reference to the "final" instruction in [Action].
 * @param exceptional Reference to the "error" instruction in [Action].
 */
public fun <Action, Result> program(
  endWith: (Result) -> Action,
  exceptional: (Throwable) -> Action,
  f: suspend ProgramBuilder<Action, Result>.() -> Result
): Action = program(ProgramBuilder(endWith, exceptional), f)

/**
 * Turns the DSL provided by a subclass [T] of [ProgramBuilder] into an actual [Action].
 */
public fun <Action, Result, T : ProgramBuilder<Action, Result>> program(
  machine: T,
  f: suspend T.() -> Result
): Action {
  f.startCoroutine(
    machine, Continuation(EmptyCoroutineContext) {
      it.fold(
        onSuccess = { result -> machine.done(result) },
        onFailure = { e -> machine.exception(e) }
      )
    }
  )
  return machine.execute()
}


