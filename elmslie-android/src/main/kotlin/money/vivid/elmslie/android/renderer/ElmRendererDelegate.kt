package money.vivid.elmslie.android.renderer

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.launch
import money.vivid.elmslie.android.elmStore
import money.vivid.elmslie.core.store.Store

@Suppress("OptionalUnit")
interface ElmRendererDelegate<Effect : Any, State : Any> {
  fun render(state: State)

  fun handleEffect(effect: Effect): Unit? = Unit

  fun mapList(state: State): List<Any> = emptyList()

  fun renderList(state: State, list: List<Any>): Unit = Unit
}

/**
 * The function makes a connection between the store and the lifecycle owner by collecting states
 * and effects and calling corresponds callbacks.
 *
 * Store creates and connects all required entities when given lifecycle reached CREATED state.
 *
 * In order to access previously saved state (via [saveState]) in [storeFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 *
 * NOTE: If you implement your own ElmRendererDelegate, you should also implement the following
 * interfaces: [ViewModelStoreOwner], [SavedStateRegistryOwner], [LifecycleOwner].
 */
@MainThread
fun <Event : Any, Effect : Any, State : Any> ElmRendererDelegate<Effect, State>.androidElmStore(
  key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
  saveState: Bundle.(State) -> Unit = {},
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
): Lazy<Store<Event, Effect, State>> {
  require(this is ViewModelStoreOwner) { "Should implement [ViewModelStoreOwner]" }
  require(this is SavedStateRegistryOwner) { "Should implement [SavedStateRegistryOwner]" }
  return androidElmStore(
    key = key,
    viewModelStoreOwner = { this },
    saveState = saveState,
    storeFactory = storeFactory,
  )
}

@MainThread
fun <Event : Any, Effect : Any, State : Any> ElmRendererDelegate<Effect, State>.androidElmStore(
  key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
  viewModelStoreOwner: () -> ViewModelStoreOwner,
  saveState: Bundle.(State) -> Unit = {},
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
): Lazy<Store<Event, Effect, State>> {
  require(this is LifecycleOwner) { "Should implement [LifecycleOwner]" }
  val lazyStore =
    elmStore(
      storeFactory = storeFactory,
      key = key,
      viewModelStoreOwner = viewModelStoreOwner,
      saveState = saveState,
    )
  with(this) {
    lifecycleScope.launch {
      withCreated {
        ElmRenderer(store = lazyStore.value, delegate = this@with, lifecycle = lifecycle)
      }
    }
  }
  return lazyStore
}
