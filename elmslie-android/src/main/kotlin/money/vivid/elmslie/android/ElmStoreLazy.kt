package money.vivid.elmslie.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import money.vivid.elmslie.core.store.Store
import money.vivid.elmslie.core.store.toCachedStore

/**
 * In order to access previously saved state (via [saveState]) in [storeFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 */
@MainThread
fun <Event : Any, Effect : Any, State : Any> Fragment.elmStore(
  key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
  viewModelStoreOwner: () -> ViewModelStoreOwner = { this },
  saveState: Bundle.(State) -> Unit = {},
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
): Lazy<Store<Event, Effect, State>> =
  money.vivid.elmslie.android.elmStore(
    storeFactory = storeFactory,
    key = key,
    viewModelStoreOwner = viewModelStoreOwner,
    saveState = saveState,
  )

/**
 * In order to access previously saved state (via [saveState]) in [storeFactory] one must use
 * SavedStateHandle.get<Bundle>(StateBundleKey)
 */
@MainThread
fun <Event : Any, Effect : Any, State : Any> ComponentActivity.elmStore(
  key: String = this::class.java.canonicalName ?: this::class.java.simpleName,
  viewModelStoreOwner: () -> ViewModelStoreOwner = { this },
  saveState: Bundle.(State) -> Unit = {},
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
): Lazy<Store<Event, Effect, State>> =
  money.vivid.elmslie.android.elmStore(
    storeFactory = storeFactory,
    key = key,
    viewModelStoreOwner = viewModelStoreOwner,
    saveState = saveState,
  )

@MainThread
internal fun <Event : Any, Effect : Any, State : Any> elmStore(
  key: String,
  viewModelStoreOwner: () -> ViewModelStoreOwner,
  saveState: Bundle.(State) -> Unit,
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
): Lazy<Store<Event, Effect, State>> =
  lazy(LazyThreadSafetyMode.NONE) {
    val factory = viewModelFactory {
      initializer {
        val savedStateHandle = createSavedStateHandle()
        RetainedElmStore(
          savedStateHandle = savedStateHandle,
          storeFactory = storeFactory,
          saveState = saveState,
        )
      }
    }

    val provider = ViewModelProvider(viewModelStoreOwner.invoke(), factory)

    @Suppress("UNCHECKED_CAST")
    provider[key, RetainedElmStore::class].store as Store<Event, Effect, State>
  }

class RetainedElmStore<Event : Any, Effect : Any, State : Any>(
  savedStateHandle: SavedStateHandle,
  storeFactory: SavedStateHandle.() -> Store<Event, Effect, State>,
  saveState: Bundle.(State) -> Unit,
) : ViewModel() {

  val store = storeFactory.invoke(savedStateHandle).toCachedStore().also { it.start() }

  init {
    savedStateHandle.setSavedStateProvider(StateBundleKey) {
      bundleOf().apply { saveState(store.states.value) }
    }
  }

  override fun onCleared() {
    store.stop()
  }

  companion object {

    const val StateBundleKey = "elm_store_state_bundle"
  }
}
