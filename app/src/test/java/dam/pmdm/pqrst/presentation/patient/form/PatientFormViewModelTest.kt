package dam.pmdm.pqrst.presentation.patient.form

import androidx.lifecycle.SavedStateHandle
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import dam.pmdm.pqrst.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [PatientFormViewModel] validation logic.
 *
 * Mechanism: the ViewModel is instantiated directly (no Hilt) with MockK-mocked repositories.
 * [SavedStateHandle] is populated with patientId=0 to simulate new-patient mode (no DB load).
 * Validation runs synchronously before any coroutine is launched, so assertions are immediate.
 */
class PatientFormViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private lateinit var repository: PatientRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var vm: PatientFormViewModel

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        authRepository = mockk(relaxed = true)
        every { authRepository.currentSession } returns MutableStateFlow(null)
        // patientId = 0L → ViewModel treats this as new-patient mode (no repository load in init)
        vm = PatientFormViewModel(
            savedStateHandle = SavedStateHandle(mapOf("patientId" to 0L)),
            repository = repository,
            authRepository = authRepository,
        )
    }

    // ── Required-field validation ─────────────────────────────────────────

    @Test
    fun `save with blank name sets nameError`() {
        // Input: name=""  →  Expected: nameError is non-null
        vm.name = ""
        vm.age  = "30"
        vm.sex  = "M"

        vm.save()

        assertNotNull(vm.nameError)
    }

    @Test
    fun `save with invalid age sets ageError`() {
        // Input: age="999" (above 150)  →  Expected: ageError is non-null
        vm.name = "Test Patient"
        vm.age  = "999"
        vm.sex  = "F"

        vm.save()

        assertNotNull(vm.ageError)
    }

    @Test
    fun `save with non-numeric age sets ageError`() {
        // Input: age="abc"  →  Expected: ageError is non-null
        vm.name = "Test Patient"
        vm.age  = "abc"
        vm.sex  = "F"

        vm.save()

        assertNotNull(vm.ageError)
    }

    @Test
    fun `save with blank sex sets sexError`() {
        // Input: sex=""  →  Expected: sexError is non-null
        vm.name = "Test Patient"
        vm.age  = "30"
        vm.sex  = ""

        vm.save()

        assertNotNull(vm.sexError)
    }

    // ── Optional-field format validation ─────────────────────────────────

    @Test
    fun `save with malformed email sets emailError`() {
        // Input: email="not-an-email"  →  Expected: emailError is non-null
        vm.name  = "Test Patient"
        vm.age   = "30"
        vm.sex   = "M"
        vm.email = "not-an-email"

        vm.save()

        assertNotNull(vm.emailError)
    }

    @Test
    fun `save with too-short phone sets phoneError`() {
        // Input: phone="12" (below minimum length)  →  Expected: phoneError is non-null
        vm.name  = "Test Patient"
        vm.age   = "30"
        vm.sex   = "M"
        vm.phone = "12"

        vm.save()

        assertNotNull(vm.phoneError)
    }

    // ── Repository interaction ────────────────────────────────────────────

    @Test
    fun `save with any validation error does not call repository`() {
        // Input: multiple invalid fields  →  Expected: repository.upsert never invoked
        vm.name = ""    // invalid
        vm.age  = "abc" // invalid
        vm.sex  = ""    // invalid

        vm.save()

        coVerify(exactly = 0) { repository.upsert(any()) }
    }

    @Test
    fun `save with all valid data clears all validation errors`() {
        // Input: all fields valid  →  Expected: every error field is null after save()
        // Verifies the synchronous validation path; repository interaction is covered by DAO integration tests.
        coEvery { repository.upsert(any()) } returns Result.success(1L)

        vm.name  = "Ana García"
        vm.age   = "35"
        vm.sex   = "F"
        vm.phone = ""               // optional, blank = valid
        vm.email = "ana@clinic.com" // valid email

        vm.save()

        assertNull(vm.nameError)
        assertNull(vm.ageError)
        assertNull(vm.sexError)
        assertNull(vm.phoneError)
        assertNull(vm.emailError)
    }
}
