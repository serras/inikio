package fp.serrano.inikio.examples

import fp.serrano.inikio.InitialStyleDSL
import fp.serrano.inikio.ProgramBuilder
import fp.serrano.inikio.program
import kotlin.coroutines.RestrictsSuspension

@InitialStyleDSL
sealed interface State<S, out A> {
  data class Finished<S, out A>(val result: A) : State<S, A>
  data class Get<S, out A>(val next: (S) -> State<S, A>) : State<S, A>
  data class Put<S, out A>(val new: S, val next: () -> State<S, A>) : State<S, A>
}

@RestrictsSuspension
class Fluflu<S, A>: ProgramBuilder<State<S, A>, A>(
  endWith = { result -> fp.serrano.inikio.examples.State.Finished(result) }
) {
  public suspend fun `get`(): S = perform { arg ->
    fp.serrano.inikio.examples.State.Get(arg) }

  public suspend fun put(new: S): Unit = performUnit { arg ->
    fp.serrano.inikio.examples.State.Put(new, arg) }
}

public fun <S, A> stato(block: suspend ProgramBuilder<State<S, A>, A>.() -> A): State<S, A> =
  program(Fluflu(), block)

fun <S, A> State<S, A>.execute(initial: S): Pair<S, A> =
  when (this) {
    is State.Finished -> initial to result
    is State.Get -> next(initial).execute(initial)
    is State.Put -> next().execute(new)
  }

fun increment(): State<Int, Unit> = state {
  put(get() + 1)
}

val stateExample = increment().execute(0)
