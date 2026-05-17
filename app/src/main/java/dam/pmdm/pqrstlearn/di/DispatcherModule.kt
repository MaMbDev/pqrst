package dam.pmdm.pqrstlearn.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Hilt qualifier for the [Dispatchers.IO] coroutine dispatcher.
 *
 * Inject with `@IoDispatcher` wherever work must run off the main thread — typically
 * Room database queries, file I/O (CSV parsing, PDF writing), and DataStore writes.
 * Using a qualifier instead of hard-coding `Dispatchers.IO` at the call site enables
 * test code to substitute a [kotlinx.coroutines.test.TestCoroutineDispatcher] for
 * deterministic, synchronous test execution.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Hilt qualifier for the [Dispatchers.Main] coroutine dispatcher.
 *
 * Inject with `@MainDispatcher` when a suspend function must resume on the Android
 * main (UI) thread, for example when updating UI state that cannot be safely mutated
 * from a background thread. In practice most UI state updates flow through
 * [kotlinx.coroutines.flow.StateFlow] collectors, which handle thread-switching
 * automatically via `collectAsStateWithLifecycle`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Hilt qualifier for an application-scoped [CoroutineScope].
 *
 * Inject with `@ApplicationScope` to launch coroutines that must outlive any single
 * ViewModel or Activity — for example, the session-restore coroutine in
 * [dam.pmdm.pqrstlearn.data.auth.AuthRepositoryImpl] which must complete even if the user
 * navigates away immediately after launch.
 *
 * Using a dedicated application scope (rather than `GlobalScope`) keeps the scope
 * injectable and testable.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module that provides coroutine dispatchers and the application-scoped
 * [CoroutineScope].
 *
 * Installed in [SingletonComponent] so all bindings are app-lifetime singletons.
 * This ensures that the same dispatcher instances are shared across all injection
 * sites, which is important for testing: replacing a dispatcher in the test Hilt
 * module automatically replaces it everywhere.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * Provides [Dispatchers.IO] for database, file, and DataStore I/O work.
     *
     * [Dispatchers.IO] maintains a pool of threads sized for blocking I/O and is
     * the correct dispatcher for Room queries, CSV parsing, and PDF generation as
     * required by RNF-02 (UI must not stutter during signal rendering).
     *
     * @return The [Dispatchers.IO] dispatcher.
     */
    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides [Dispatchers.Main] for work that must execute on the Android UI thread.
     *
     * @return The [Dispatchers.Main] dispatcher.
     */
    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provides a singleton [CoroutineScope] tied to the application lifetime.
     *
     * **Why [SupervisorJob]?** A `SupervisorJob` ensures that if one child coroutine
     * fails (e.g. the session-restore job throws), the failure does not propagate to
     * sibling coroutines or cancel the parent scope. This is the correct behaviour for
     * an application-wide scope where unrelated jobs must continue to run.
     *
     * **Why [Dispatchers.Default]?** The scope's default dispatcher is
     * [Dispatchers.Default] (CPU-bound work). Individual coroutines launched within
     * this scope use [withContext] or their own dispatcher as needed (e.g. the
     * session-restore coroutine explicitly switches to [IoDispatcher]).
     *
     * @return A [CoroutineScope] backed by [SupervisorJob] + [Dispatchers.Default].
     */
    @Singleton
    @ApplicationScope
    @Provides
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
