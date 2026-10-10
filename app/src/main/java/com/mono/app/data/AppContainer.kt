package com.mono.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow

/** Tiny manual DI container. */
class AppContainer(context: Context) {
    val repo = MonoRepository(MonoDatabase.build(context))
    /** One-line user-facing messages shown as snackbars. */
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Runs [block]; on failure tells the user and returns false. Never fakes success. */
    suspend fun guarded(errorMessage: String, block: suspend () -> Unit): Boolean = try {
        block(); true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        messages.tryEmit(errorMessage); false
    }
}
