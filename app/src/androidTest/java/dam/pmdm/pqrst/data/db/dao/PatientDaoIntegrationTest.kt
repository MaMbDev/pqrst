package dam.pmdm.pqrst.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dam.pmdm.pqrst.data.db.PqrstDatabase
import dam.pmdm.pqrst.data.db.entity.PatientEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [PatientDao] against a real in-memory Room database.
 *
 * Mechanism: Room.inMemoryDatabaseBuilder builds a fully functional SQLite DB.
 * FK constraints are disabled so test fixtures can be inserted without a matching user row.
 * Each test runs in its own isolated DB instance (setUp/tearDown).
 */
@RunWith(AndroidJUnit4::class)
class PatientDaoIntegrationTest {

    private lateinit var db: PqrstDatabase
    private lateinit var dao: PatientDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PqrstDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        // Disable FK constraints so we can insert patients without a real users row
        db.openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = OFF")
        dao = db.patientDao()
    }

    @After
    fun tearDown() = db.close()

    // ── Helper ────────────────────────────────────────────────────────────

    private fun patient(
        id: Long = 0,
        name: String,
        age: Int = 30,
        isActive: Int = 1,
    ) = PatientEntity(
        id = id,
        name = name,
        age = age,
        sex = "M",
        medicalHistory = null,
        phone = null,
        email = null,
        createdAt = "2024-01-01T00:00:00",
        isActive = isActive,
        createdBy = 0,
    )

    // ── Insert + observe ──────────────────────────────────────────────────

    @Test
    fun insertPatient_observeAll_returnsInsertedRow() = runBlocking {
        // Input: insert one patient
        // Expected: observeAll emits a list of size 1 with that patient's name
        dao.insert(patient(name = "María García"))

        val result = dao.observeAll().first()

        assertEquals(1, result.size)
        assertEquals("María García", result[0].name)
    }

    @Test
    fun insertMultiplePatients_observeAll_orderedById() = runBlocking {
        // Input: insert two patients (second has lower natural-sort name but higher id)
        // Expected: list ordered by id ASC
        dao.insert(patient(id = 2, name = "Carlos Ruiz"))
        dao.insert(patient(id = 1, name = "Ana López"))

        val result = dao.observeAll().first()

        assertEquals(1L, result[0].id)
        assertEquals(2L, result[1].id)
    }

    // ── Name search ───────────────────────────────────────────────────────

    @Test
    fun observeAll_nameQuery_returnsOnlyContainsMatches() = runBlocking {
        // Input: two patients, query = "Ana"
        // Expected: only the patient whose name contains "Ana"
        dao.insert(patient(name = "Ana López"))
        dao.insert(patient(name = "Carlos Ruiz"))

        val result = dao.observeAll(query = "Ana").first()

        assertEquals(1, result.size)
        assertEquals("Ana López", result[0].name)
    }

    @Test
    fun observeAll_nameQuery_noMatch_returnsEmpty() = runBlocking {
        // Input: one patient, query has no match
        // Expected: empty list
        dao.insert(patient(name = "María García"))

        val result = dao.observeAll(query = "XYZ").first()

        assertTrue(result.isEmpty())
    }

    // ── ID search (starts-with, not contains) ─────────────────────────────

    @Test
    fun observeAll_idQuery_matchesStartsWithOnly_notContains() = runBlocking {
        // Input: patients with id=3, id=13, id=30.  Query = "3"
        // Expected: id=3 and id=30 match (start with "3"); id=13 does NOT match
        dao.insert(patient(id = 3,  name = "Alice"))
        dao.insert(patient(id = 13, name = "Bob"))
        dao.insert(patient(id = 30, name = "Charlie"))

        val result = dao.observeAll(query = "3").first()
        val ids = result.map { it.id }

        assertTrue("id=3 should match",  ids.contains(3))
        assertTrue("id=30 should match", ids.contains(30))
        assertFalse("id=13 must NOT match (contains, not starts-with)", ids.contains(13))
    }

    @Test
    fun observeAll_idQuery_prefix1_includesAllStartingWith1() = runBlocking {
        // Input: patients with id=10, id=12, id=21.  Query = "1"
        // Expected: id=10 and id=12 match; id=21 does NOT match
        dao.insert(patient(id = 10, name = "Dave"))
        dao.insert(patient(id = 12, name = "Eve"))
        dao.insert(patient(id = 21, name = "Frank"))

        val result = dao.observeAll(query = "1").first()
        val ids = result.map { it.id }

        assertTrue("id=10 should match",  ids.contains(10))
        assertTrue("id=12 should match",  ids.contains(12))
        assertFalse("id=21 must NOT match", ids.contains(21))
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @Test
    fun deleteById_removesPatient_fromObservedList() = runBlocking {
        // Input: insert then delete a patient
        // Expected: observeAll returns empty list
        val id = dao.insert(patient(name = "To Delete"))

        dao.deleteById(id)

        assertTrue(dao.observeAll().first().isEmpty())
    }

    // ── Soft delete ───────────────────────────────────────────────────────

    @Test
    fun observeAll_softDeletedPatient_isExcluded() = runBlocking {
        // Input: one active (isActive=1) and one soft-deleted (isActive=0) patient
        // Expected: only the active patient is returned
        dao.insert(patient(name = "Active Patient",   isActive = 1))
        dao.insert(patient(name = "Inactive Patient", isActive = 0))

        val result = dao.observeAll().first()

        assertEquals(1, result.size)
        assertEquals("Active Patient", result[0].name)
    }

    // ── getById ───────────────────────────────────────────────────────────

    @Test
    fun getById_existingId_returnsCorrectPatient() = runBlocking {
        // Input: insert a patient, retrieve by the generated id
        // Expected: returned entity matches inserted data
        val generatedId = dao.insert(patient(name = "Lookup Patient", age = 42))

        val found = dao.getById(generatedId)

        assertNotNull(found)
        assertEquals("Lookup Patient", found!!.name)
        assertEquals(42, found.age)
    }
}
