package money.vivid.elmslie.core.store.dsl

@DslMarker internal annotation class OperationsBuilderDsl

@OperationsBuilderDsl
class OperationsBuilder<T : Any> {

  private val list = mutableListOf<T>()

  operator fun T?.unaryPlus() {
    this?.let(list::add)
  }

  operator fun Collection<T>.unaryPlus() {
    list.addAll(this)
  }

  internal fun build() = list
}
