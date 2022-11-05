package fp.serrano.inikio.ksp

class BuilderWrapper<A>(
  var current: A
) {
  fun `do`(f: A.() -> A) {
    current = f(current)
  }

  fun <B> end(f: A.() -> B): B = f(current)

  companion object {
    operator fun <A, B> invoke(x: A, end: A.() -> B, block: BuilderWrapper<A>.() -> Unit): B =
      BuilderWrapper(x).also(block).end(end)
  }
}