package fp.serrano.inikio.examples

import fp.serrano.inikio.plugin.InitialStyleDSL

@InitialStyleDSL
sealed interface State<S, out A> {
  data class Finished<S, out A>(val result: A) : State<S, A>
  data class Get<S, out A>(val next: (S) -> State<S, A>) : State<S, A>
  data class Put<S, out A>(val new: S, val next: () -> State<S, A>) : State<S, A>
}

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
