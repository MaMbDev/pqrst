package dam.pmdm.pqrst.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dam.pmdm.pqrst.domain.model.Session
import dam.pmdm.pqrst.presentation.admin.users.form.UserFormScreen
import dam.pmdm.pqrst.presentation.admin.users.list.UserListScreen
import dam.pmdm.pqrst.presentation.auth.login.LoginScreen
import dam.pmdm.pqrst.presentation.consultation.detail.ConsultationDetailScreen
import dam.pmdm.pqrst.presentation.consultation.form.ConsultationFormScreen
import dam.pmdm.pqrst.presentation.consultation.list.ConsultationListScreen
import dam.pmdm.pqrst.presentation.dashboard.DashboardScreen
import dam.pmdm.pqrst.presentation.ecg.analysis.EcgAnalysisScreen
import dam.pmdm.pqrst.presentation.ecg.importcsv.EcgImportScreen
import dam.pmdm.pqrst.presentation.ecg.monitor.EcgMonitorScreen
import dam.pmdm.pqrst.presentation.patient.detail.PatientDetailScreen
import dam.pmdm.pqrst.presentation.patient.form.PatientFormScreen
import dam.pmdm.pqrst.presentation.patient.list.PatientListScreen
import dam.pmdm.pqrst.presentation.report.ReportPreviewScreen
import dam.pmdm.pqrst.presentation.learn.EcgGuideScreen
import dam.pmdm.pqrst.presentation.learn.HeartAnatomyScreen
import dam.pmdm.pqrst.presentation.settings.AboutScreen
import dam.pmdm.pqrst.presentation.settings.SettingsScreen

/**
 * Root navigation graph for the PQRST Learn application.
 *
 * **Overall structure**
 * The graph uses type-safe routes (Kotlin serializable objects/data classes) defined in
 * [Routes.kt]. The start destination is [Login]; after a successful session restore the
 * [LaunchedEffect] immediately replaces the stack with [Dashboard].
 *
 * **Auth-driven navigation**
 * A single [LaunchedEffect] keyed on [session] and [isCheckingSession] acts as the
 * authoritative navigation decision:
 * - While [isCheckingSession] is true (DataStore read in progress) no navigation is
 *   performed to avoid a flash of the wrong screen.
 * - When the session becomes non-null the entire back stack is replaced with [Dashboard]
 *   so the user cannot press Back to reach [Login].
 * - When the session becomes null (logout) the stack is replaced with [Login] for the
 *   same reason.
 *
 * Individual screens do not need to duplicate this logic; they only need to call
 * [onLogout] which sets the session to null in the repository and triggers the effect.
 *
 * **Drawer navigation**
 * Screens that contain a navigation drawer pass a `onDrawerNavigate` callback. String
 * route keys are used here (rather than typed routes) because the drawer items map to a
 * fixed set of top-level destinations and the stringly-typed approach avoids wrapping
 * each destination in a sealed class.
 *
 * **Route inventory**
 * | Route              | Screen                     | Args                              |
 * |--------------------|----------------------------|-----------------------------------|
 * | [Login]            | LoginScreen                | —                                 |
 * | [Dashboard]        | DashboardScreen            | session (passed as param)         |
 * | [PatientList]      | PatientListScreen          | —                                 |
 * | [PatientDetail]    | PatientDetailScreen        | patientId: Long                   |
 * | [PatientForm]      | PatientFormScreen          | patientId: Long (0 = new)         |
 * | [ConsultationList] | ConsultationListScreen     | —                                 |
 * | [ConsultationDetail]| ConsultationDetailScreen  | consultationId: Long              |
 * | [ConsultationForm] | ConsultationFormScreen     | patientId: Long, consultationId: Long (0 = new) |
 * | [EcgMonitor]       | EcgMonitorScreen           | consultationId: Long              |
 * | [EcgImport]        | EcgImportScreen            | consultationId: Long              |
 * | [EcgAnalysis]      | EcgAnalysisScreen          | ecgRecordId: Long                 |
 * | [ReportPreview]    | ReportPreviewScreen        | consultationId: Long, ecgRecordId: Long (0 = none) |
 * | [UserList]         | UserListScreen (ADMIN)     | —                                 |
 * | [UserForm]         | UserFormScreen (ADMIN)     | userId: Long (0 = new)            |
 * | [Settings]         | SettingsScreen             | —                                 |
 * | [About]            | AboutScreen                | —                                 |
 * | [EcgGuide]         | EcgGuideScreen             | —                                 |
 * | [HeartAnatomy]     | HeartAnatomyScreen         | —                                 |
 *
 * @param session The currently authenticated user's session, or null when logged out.
 * @param isCheckingSession True while the initial DataStore session-restore check is in progress.
 *                          Navigation is suppressed until this flag becomes false.
 * @param onLogout Callback that clears the session in the repository, triggering
 *                 re-navigation to [Login] via the [LaunchedEffect].
 * @param navController The [NavHostController] managing the back stack.
 *                      Defaults to a new controller created with [rememberNavController].
 */
@Composable
fun PqrstNavGraph(
    session: Session?,
    isCheckingSession: Boolean,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Single source of truth for auth-driven navigation.
    // Keying on both session and isCheckingSession ensures we re-evaluate whenever
    // either changes, but we skip while the check is still in progress.
    LaunchedEffect(session, isCheckingSession) {
        if (isCheckingSession) return@LaunchedEffect
        if (session != null) {
            // Replace the entire back stack so Back cannot return to Login.
            navController.navigate(Dashboard) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate(Login) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Login) {

        // ── Login ──────────────────────────────────────────────────────────────
        // onLoginSuccess is a no-op: the LaunchedEffect above handles navigation on session change.
        composable<Login> {
            LoginScreen(onLoginSuccess = {})
        }

        // ── Dashboard ──────────────────────────────────────────────────────────
        // Translates string drawer-navigation keys to typed route objects.
        composable<Dashboard> {
            DashboardScreen(
                session = session,
                onLogout = onLogout,
                onNavigate = { route ->
                    when (route) {
                        "patients" -> navController.navigate(PatientList)
                        "consultations" -> navController.navigate(ConsultationList)
                        "ecg_monitor" -> navController.navigate(EcgMonitor(0L))
                        "ecg_import" -> navController.navigate(EcgImport(0L))
                        "users" -> navController.navigate(UserList)
                        "settings" -> navController.navigate(Settings)
                        "about" -> navController.navigate(About)
                        "ecg_guide" -> navController.navigate(EcgGuide)
                        "heart_anatomy" -> navController.navigate(HeartAnatomy)
                        else -> Unit
                    }
                },
            )
        }

        // ── Patient list ───────────────────────────────────────────────────────
        composable<PatientList> {
            PatientListScreen(
                session = session,
                onBack = { navController.navigateUp() },
                onLogout = onLogout,
                onNavigateToDetail = { navController.navigate(PatientDetail(it)) },
                onNavigateToForm = { navController.navigate(PatientForm(it ?: 0L)) },
                onDrawerNavigate = { route ->
                    when (route) {
                        "dashboard" -> navController.navigate(Dashboard) {
                            popUpTo(PatientList) { inclusive = true }
                        }
                        "consultations" -> navController.navigate(ConsultationList)
                        "users" -> navController.navigate(UserList)
                        "settings" -> navController.navigate(Settings)
                        "about" -> navController.navigate(About)
                        "ecg_monitor" -> navController.navigate(EcgMonitor(0L))
                        "ecg_import" -> navController.navigate(EcgImport(0L))
                        else -> Unit
                    }
                },
            )
        }

        // ── Consultation list ──────────────────────────────────────────────────
        composable<ConsultationList> {
            ConsultationListScreen(
                session = session,
                onBack = { navController.navigateUp() },
                onLogout = onLogout,
                onNavigateToDetail = { navController.navigate(ConsultationDetail(it)) },
                onNewConsultation = { patientId -> navController.navigate(ConsultationForm(patientId)) },
                onDrawerNavigate = { route ->
                    when (route) {
                        "dashboard" -> navController.navigate(Dashboard) {
                            popUpTo(ConsultationList) { inclusive = true }
                        }
                        "patients" -> navController.navigate(PatientList)
                        "users" -> navController.navigate(UserList)
                        "settings" -> navController.navigate(Settings)
                        "about" -> navController.navigate(About)
                        "ecg_monitor" -> navController.navigate(EcgMonitor(0L))
                        "ecg_import" -> navController.navigate(EcgImport(0L))
                        else -> Unit
                    }
                },
            )
        }

        // ── Patient detail ─────────────────────────────────────────────────────
        // args.patientId is passed to both the detail screen and the edit-form route.
        composable<PatientDetail> { back ->
            val args = back.toRoute<PatientDetail>()
            PatientDetailScreen(
                patientId = args.patientId,
                onBack = { navController.popBackStack() },
                onEditPatient = { navController.navigate(PatientForm(args.patientId)) },
                onConsultationClick = { navController.navigate(ConsultationDetail(it)) },
                onNewConsultation = { navController.navigate(ConsultationForm(args.patientId)) },
            )
        }

        // ── Patient form (create / edit) ───────────────────────────────────────
        // patientId 0 → create mode; non-zero → edit mode.
        composable<PatientForm> { back ->
            val args = back.toRoute<PatientForm>()
            PatientFormScreen(
                patientId = args.patientId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ── Consultation detail ────────────────────────────────────────────────
        composable<ConsultationDetail> { back ->
            val args = back.toRoute<ConsultationDetail>()
            ConsultationDetailScreen(
                consultationId = args.consultationId,
                onBack = { navController.popBackStack() },
                onEditConsultation = { patientId ->
                    navController.navigate(ConsultationForm(patientId, args.consultationId))
                },
                onEcgAnalysis = { navController.navigate(EcgAnalysis(it)) },
                onEcgMonitor = { navController.navigate(EcgMonitor(args.consultationId)) },
                onEcgImport = { navController.navigate(EcgImport(args.consultationId)) },
                onReport = { navController.navigate(ReportPreview(args.consultationId)) },
            )
        }

        // ── Consultation form (create / edit) ──────────────────────────────────
        composable<ConsultationForm> { back ->
            val args = back.toRoute<ConsultationForm>()
            ConsultationFormScreen(
                patientId = args.patientId,
                consultationId = args.consultationId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ── ECG monitor ────────────────────────────────────────────────────────
        // consultationId 0 → opened from the dashboard (no linked consultation).
        composable<EcgMonitor> { back ->
            val args = back.toRoute<EcgMonitor>()
            EcgMonitorScreen(
                consultationId = args.consultationId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ── ECG CSV import ─────────────────────────────────────────────────────
        composable<EcgImport> { back ->
            val args = back.toRoute<EcgImport>()
            EcgImportScreen(
                consultationId = args.consultationId,
                onBack = { navController.popBackStack() },
                onImported = { navController.popBackStack() },
            )
        }

        // ── ECG analysis ───────────────────────────────────────────────────────
        composable<EcgAnalysis> { back ->
            val args = back.toRoute<EcgAnalysis>()
            EcgAnalysisScreen(
                ecgRecordId = args.ecgRecordId,
                onBack = { navController.popBackStack() },
            )
        }

        // ── Report preview ─────────────────────────────────────────────────────
        // ecgRecordId 0 → report generated without ECG section.
        composable<ReportPreview> { back ->
            val args = back.toRoute<ReportPreview>()
            ReportPreviewScreen(
                consultationId = args.consultationId,
                ecgRecordId = args.ecgRecordId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
            )
        }

        // ── User list (ADMIN only) ─────────────────────────────────────────────
        composable<UserList> {
            UserListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { navController.navigate(UserForm(it ?: 0L)) },
            )
        }

        // ── User form (ADMIN only) ─────────────────────────────────────────────
        composable<UserForm> { back ->
            val args = back.toRoute<UserForm>()
            UserFormScreen(
                userId = args.userId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ── Settings ───────────────────────────────────────────────────────────
        composable<Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // ── About ──────────────────────────────────────────────────────────────
        composable<About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        // ── ECG waveform educational guide ─────────────────────────────────────
        composable<EcgGuide> {
            EcgGuideScreen(onBack = { navController.popBackStack() })
        }

        // ── Heart anatomy / conduction system guide ────────────────────────────
        composable<HeartAnatomy> {
            HeartAnatomyScreen(onBack = { navController.popBackStack() })
        }
    }
}
