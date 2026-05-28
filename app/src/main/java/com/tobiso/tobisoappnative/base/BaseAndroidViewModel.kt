package com.tobiso.tobisoappnative.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

/**
 * Use this as base class when you need Application context (e.g. for NetworkUtils, DataStore).
 * For ViewModels that don't need context, use [BaseViewModel] instead.
 *
 * Pass [savedStateHandle] to persist critical state across process death.
 * Hilt injects the handle automatically; pass it through from the concrete ViewModel.
 */
abstract class BaseAndroidViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    application: Application,
    initialState: S,
    protected val savedStateHandle: SavedStateHandle? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    abstract fun onIntent(intent: I)

    protected val currentState: S
        get() = _uiState.value

    protected fun setState(reducer: S.() -> S) {
        _uiState.update { it.reducer() }
    }

    protected fun emitEffect(effect: E) {
        viewModelScope.launch { _effect.send(effect) }
    }
}
