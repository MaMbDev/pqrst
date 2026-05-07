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
 * Acts as the single source of truth for auth-driven navigation: a [LaunchedEffect] observes
 * [session] and [isCheckingSession] and replaces the entire back stack with either [Dashboard]
 * or [Login] whenever the authentication state changes. This means individual screens do not
 * need to handle auth redirects themselves.
 *
 * @param session The currently authenticated user's session, or null when logged out.
 * @param isCheckingSession True while the initial DataStore session-restore check is in progress.
 *                          Navigation is suppressed until this flag becomes false.
 * @param onLogout Callback that clears the session in the repository; triggers re-navigation to [Login].
 * @param navController The [NavHostController] managing the back stack. Defaults to a new instance.
 */
@Composable
fun PqrstNavGraph(
    session: Session?,
    isCheckingSession: Boolean,
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(session, isCheckingSession) {
        if (isCheckingSession) return@LaunchedEffect
        if (session != null) {
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

        composable<Login> {
            /* onLoginSuccess is a no-op: the LaunchedEffect above handles navigation on session change. */
            LoginScreen(onLoginSuccess = {})
        }

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

        composable<ConsultationList> {
            ConsultationListScreen(
                session = session,
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

        composable<PatientForm> { back ->
            val args = back.toRoute<PatientForm>()
            PatientFormScreen(
                patientId = args.patientId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

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

        composable<ConsultationForm> { back ->
            val args = back.toRoute<ConsultationForm>()
            ConsultationFormScreen(
                patientId = args.patientId,
                consultationId = args.consultationId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<EcgMonitor> { back ->
            val args = back.toRoute<EcgMonitor>()
            EcgMonitorScreen(
                consultationId = args.consultationId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<EcgImport> { back ->
            val args = back.toRoute<EcgImport>()
            EcgImportScreen(
                consultationId = args.consultationId,
                onBack = { navController.popBackStack() },
                onImported = { navController.popBackStack() },
            )
        }

        composable<EcgAnalysis> { back ->
            val args = back.toRoute<EcgAnalysis>()
            EcgAnalysisScreen(
                ecgRecordId = args.ecgRecordId,
                onBack = { navController.popBackStack() },
            )
        }

        composable<ReportPreview> { back ->
            val args = back.toRoute<ReportPreview>()
            ReportPreviewScreen(
                consultationId = args.consultationId,
                ecgRecordId = args.ecgRecordId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
            )
        }

        composable<UserList> {
            UserListScreen(
                onBack = { navController.popBackStack() },
                onNavigateToForm = { navController.navigate(UserForm(it ?: 0L)) },
            )
        }

        composable<UserForm> { back ->
            val args = back.toRoute<UserForm>()
            UserFormScreen(
                userId = args.userId.takeIf { it != 0L },
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable<Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable<About> {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable<EcgGuide> {
            EcgGuideScreen(onBack = { navController.popBackStack() })
        }

        composable<HeartAnatomy> {
            HeartAnatomyScreen(onBack = { navController.popBackStack() })
        }
    }
}
