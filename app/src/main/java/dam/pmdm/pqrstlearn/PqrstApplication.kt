package dam.pmdm.pqrstlearn

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the PQRST Learn app.
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation and initialise
 * the dependency injection graph at process start. This makes [PqrstApplication] the
 * root Hilt component from which all other scoped components ([SingletonComponent],
 * [ActivityComponent], [ViewModelComponent], etc.) are derived.
 *
 * **Why a custom [Application] class?** Android requires a custom [Application]
 * subclass to be declared in the manifest when using Hilt. Without it, Hilt cannot
 * inject into Activities, Fragments, or ViewModels. The class itself contains no
 * additional logic; all initialisation work is handled by the Hilt-generated
 * `Hilt_PqrstApplication` base class and the individual Hilt modules.
 *
 * **Why [HiltAndroidApp] instead of manual component initialisation?**
 * [HiltAndroidApp] generates all the boilerplate needed to connect the Dagger component
 * graph to the Android lifecycle automatically. Manual Dagger component initialisation
 * would require significantly more code and would not integrate with Hilt's
 * `@AndroidEntryPoint` and `@HiltViewModel` annotations used throughout the app.
 */
@HiltAndroidApp
class PqrstApplication : Application()
