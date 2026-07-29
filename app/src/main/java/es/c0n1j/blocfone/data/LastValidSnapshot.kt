package es.c0n1j.blocfone.data

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class LastValidSnapshot<T> {
    private val value = AtomicReference<T?>(null)

    fun update(snapshot: T): T {
        value.set(snapshot)
        return snapshot
    }

    fun current(): T? = value.get()
}

internal class ReadRetryController {
    private val mutableAttempts = MutableStateFlow(0L)

    val attempts: StateFlow<Long> = mutableAttempts.asStateFlow()

    fun retry() {
        mutableAttempts.update { current -> current + 1 }
    }
}
