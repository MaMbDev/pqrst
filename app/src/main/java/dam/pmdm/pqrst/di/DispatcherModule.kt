package dam.pmdm.pqrst.di

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
 * Inject with `@IoDispatcher` to run database, network, or file I/O off the main thread.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Hilt qualifier for the [Dispatchers.Main] coroutine dispatcher.
 *
 * Inject with `@MainDispatcher` when work must run on the Android main thread.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Hilt qualifier for an application-scoped [CoroutineScope].
 *
 * Inject with `@ApplicationScope` to launch coroutines that must outlive any single
 * ViewModel or Activity (e.g. the session-restore coroutine in [dam.pmdm.pqrst.data.auth.AuthRepositoryImpl]).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module that provides coroutine dispatchers and the application-scoped coroutine scope.
 *
 * Installed in [SingletonComponent] so all bindings are app-lifetime singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    /**
     * Provides [Dispatchers.IO] for database and file I/O work.
     *
     * @return The [Dispatchers.IO] dispatcher.
     */
    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * Provides [Dispatchers.Main] for work that must run on the UI thread.
     *
     * @return The [Dispatchers.Main] dispatcher.
     */
    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    /**
     * Provides a singleton [CoroutineScope] tied to the application lifetime.
     *
     * Uses [SupervisorJob] so a child failure does not cancel sibling coroutines.
     *
     * @return A [CoroutineScope] backed by [SupervisorJob] and [Dispatchers.Default].
     */
    @Singleton
    @ApplicationScope
    @Provides
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
