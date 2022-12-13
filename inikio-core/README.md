# Module Inikio

> Better initial-style DSLs in Kotlin &nbsp;&nbsp; <a href="https://github.com/serras/inikio">
<img src="https://badgen.net/github/stars/serras/inikio?style=social&label=GitHub stars" style="display: inline;" />
</a> <a href="https://github.com/serras/inikio/releases/">
<img src="https://badgen.net/github/release/serras/inikio?style=social&label=Latest release" style="display: inline;" />
</a>

<br />

* <a href="#initial-style-dsls">Initial-style DSLs</a>
  * <a href="#execution">Execution</a>
* <a href="#suspended-syntax"><code>suspend</code>ed syntax</a>
  * <a href="-inikio/fp.serrano.inikio.plugin/index.html">Compiler plug-in</a>
* <a href="https://github.com/serras/inikio/tree/main/inikio-examples/src/commonMain/kotlin/fp/serrano/inikio/examples">Examples of DSLs</a>

<h3 id="gradle-setup">Gradle set-up</h3>

Inikio is available through [Jitpack](https://jitpack.io/).

```kotlin
repositories {
  maven(url = "https://jitpack.io")
}
dependencies {
  implementation("com.github.serras.inikio:inikio-core:$inikioVersion")
}
```

<h3 id="initial-style-dsls">Initial-style DSLs</h3>

Sometimes you need to model actions or behaviors as part of your model. For example, [rules for filtering data](https://engineering.fb.com/2015/06/26/security/fighting-spam-with-haskell/), [smart contracts](https://github.com/epfl-lara/smart/blob/master/core/src/sphinx/smartcontracts.rst), or [trading card games](https://serranofp.com/zurihac-workshop/). In most cases these actions come from a particular language, called a _domain-specific language_ (DSL for short). Some people separate those actions from the programming language were they are used by means tools like [JetBrains MPS](https://www.jetbrains.com/mps/), but here we're concerned with actually representing those actions in our favorite programming language, Kotlin.

There are different patterns to embed DSLs in an existing language. _Initial-style_ is one of them, referring to the case in which the actions are represented as _data_. Here's an example showing the basic elements of this pattern:

```kotlin
sealed interface Casino<out A>

data class Done<out A>(val result: A): Casino<A>
data class FlipCoin<out A>(val next: (Outcome) -> Casino<A>): Casino<A> {
  enum class Outcome { HEADS, TAILS }
}
```

1. An interface `Casino` which forms the top of the sealed hierarchy.
2. A few _primitive actions_; in this case only one, `FlipCoin`. Those actions from the basis of what you can express using your language.
3. A data class which "ends" the actions; in this case `Done`.

Each of the primitive actions in (2) also follow a particular shape. Their last argument is a _continuation_, a function which specifies the "next" action to be executed after this one. That function has as argument the type of data which is "consumed" by the action, and always refers back to the top of the sealed hierarchy as result.

Using `Casino` we can express different games which depend on flipping coins. For example, here's a game in which you flip two coins, and you win whenever both are heads. Kotlin's ability to drop parentheses for a block argument gives us nice syntax to express what to do next after each `FlipCoin`.

```kotlin
val doubleCoin =
  FlipCoin { o1 ->
    FlipCoin { o2 ->
      if (o1 == Outcome.HEADS && o2 == Outcome.HEADS) 
        Done(WIN)
      else
        Done(LOSE)
    }
  }
```

<h4 id="execution">Execution</h4>

Values of that initial-style DSL can be _executed_ in different ways (sometimes we also say they are _interpreted_). In fact, the main advantage of describing actions using this pattern is that writing those interpretations is much simpler than in others. In most cases, you have a big `when` to handle each of the primitive instructions, which calls itself (tail recursively) with the continuation.

```kotlin
tailrec fun <A> Casino<A>.execute(): A =
  when(this) {
    is Done -> result
    is FlipCoin -> next(Random.nextFlipOutcome()).execute()
  }

fun Random.nextFlipOutcome(): FlipCoin.Outcome =
  if (nextBoolean()) FlipCoin.Outcome.HEADS else FlipCoin.Outcome.TAILS
```

<h3 id="suspended-syntax"><code>suspend</code>ed syntax</h3>

As useful as it is for writing interpretations, initial-style DSLs suffer from a not-so-nice interface for creating values. The `doubleCoin` above is a prime example: you need to nest everytime you use a primitive action, and remember to wrap the final result with `Done`. Conceptually, though, that problem is a _sequence_ of operations, and we would like that to be reflected in the way we write our code.

Fortunately, [Kotlin's coroutine system](https://kotlinlang.org/docs/coroutines-overview.html) gives us enough tools to fulfill our wishes. Inikio just wraps them in a nice package, and provides a small compiler plug-in for the boilerplate we need to write. The main idea is to have a `Builder` in which each primitive operation is represented as a `suspend`ed method, and a runner which turns the `Builder` into the actual initial-style DSL. 

```kotlin
// the builder class
class CasinoBuilder<A>: ProgramBuilder<Casino<A>, A> {
  suspend fun flipCoin(): FlipCoin.Outcome
}
// the runner
fun <A> casino(block: CasinoBuilder<A>.() -> A): Casino<A>
```

The `doubleCoin` example can be re-written as follows.

```kotlin
val doubleCoin = casino {
  val o1 = flipCoin()
  val o2 = flipCoin()
  if (o1 == Outcome.HEADS && o2 == Outcome.HEADS) WIN
  else LOSE
}
```

At the core of this technique we have the `ProgramBuilder` class, which implements a state machine on top of coroutines (thanks to [Simon Vergauwen](https://twitter.com/vergauwen_simon/) and [Raúl Raja](https://twitter.com/raulraja/) for teaching it to me!). The details are not important, but the general idea is that by turning each primitive operation into a `suspend`ed function, the runner can "detect" when it's called, and produce the corresponding data class instance from the initial-style DSL.

The great news is that you can get all the benefits of this nicer style without having to write anything else than the initial-style DSL. The <a href="-inikio/fp.serrano.inikio.plugin/index.html">compiler plug-in</a> generates the `Builder` and the runner for you.

# Package fp.serrano.inikio

The core `ProgramBuilder` and corresponding runner. Every DSL generated by the plug-in is based on these two.

# Package fp.serrano.inikio.plugin

Information about Inikio's compiler plug-in, that creates `Builder`s automatically for your initial-style DSLs.

### Step 1, add the plug-in to your build

The plug-in is based on [KSP](https://kotlinlang.org/docs/ksp-overview.html). If you are using Gradle you need to add the following to your build file. <a href="https://github.com/serras/inikio/releases/">
<img src="https://badgen.net/github/release/serras/inikio?style=social&label=inikioVersion" style="display: inline;" />
</a>

```kotlin
repositories {
  mavenCentral()
  maven(url = "https://jitpack.io")
}
plugins {
  id("com.google.devtools.ksp") version "1.7.22-1.0.8"
}
dependencies {
  implementation("com.github.serras.inikio:inikio-core:$inikioVersion")
  ksp("com.github.serras.inikio:inikio-ksp:$inikioVersion")
}
```

If IntelliJ is your IDE of choice, we recommend [configuring your build to make it aware of KSP](https://kotlinlang.org/docs/ksp-quickstart.html#make-ide-aware-of-generated-code).

### Step 2, annotate your DSLs

You only need to add the `@InitialStyleDSL` annotation to the top of your hierarchy. Remember that you need to have one "finished" variant, in the example below is `Done`.

```kotlin
@InitialStyleDSL
sealed interface Casino<out A>
data class Done<out A>(val result: A): Casino<A>
data class FlipCoin<out A>(val next: (Outcome) -> Casino<A>): Casino<A> {
  enum class Outcome { HEADS, TAILS }
}
```

### Step 3, enjoy your new `Builder`

From the definition above the plug-in generates a `Builder` class and a runner function.

- The `Builder` class contains a method for each variant in the DSL, that is, for each basic instruction in your DSL.

    ```kotlin
    class CasinoBuilder<A> {
      suspend fun flipCoin(): FlipCoin.Outcome
    }
    ```

- The runner function takes a block with the `Builder` as receiver, and converts it into the initial-style DSL.

    ```kotlin
    fun <A> casino(block: CasinoBuilder<A>.() -> A): Casino<A>
    ```
  
You can use the combination of the runner and the `Builder` methods to create values of your initial-style DSL. For example, the following defines a game when only two heads win.

```kotlin
val doubleCoin = casino {
  val o1 = flipCoin()
  val o2 = flipCoin()
  if (o1 == Outcome.HEADS && o2 == Outcome.HEADS) WIN
  else LOSE
}
```

Note the much nicer syntax with `suspend` that what you'd get with the data classes themselves. In particular, all the nesting is gone, and there's no need to call the final `Done`. The code above is equivalent to the following.

```kotlin
val casino =
  FlipCoin { o1 ->
    FlipCoin { o2 ->
      if (o1 == Outcome.HEADS && o2 == Outcome.HEADS) 
        Done(WIN)
      else
        Done(LOSE)
    }
  }
```