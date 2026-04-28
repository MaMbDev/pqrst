package dam.pmdm.pqrst

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the PQRST Learn app.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and act as
 * the root component for dependency injection throughout the application lifecycle.
 */
@HiltAndroidApp
class PqrstApplication : Application()
