package dam.pmdm.pqrstlearn.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dam.pmdm.pqrstlearn.data.auth.AuthRepositoryImpl
import dam.pmdm.pqrstlearn.data.repository.ConsultationRepositoryImpl
import dam.pmdm.pqrstlearn.data.repository.EcgRepositoryImpl
import dam.pmdm.pqrstlearn.data.repository.PatientRepositoryImpl
import dam.pmdm.pqrstlearn.data.repository.ReportRepositoryImpl
import dam.pmdm.pqrstlearn.data.repository.UserRepositoryImpl
import dam.pmdm.pqrstlearn.domain.repository.AuthRepository
import dam.pmdm.pqrstlearn.domain.repository.ConsultationRepository
import dam.pmdm.pqrstlearn.domain.repository.EcgRepository
import dam.pmdm.pqrstlearn.domain.repository.PatientRepository
import dam.pmdm.pqrstlearn.domain.repository.ReportRepository
import dam.pmdm.pqrstlearn.domain.repository.UserRepository
import javax.inject.Singleton

/**
 * Hilt module that binds domain repository interfaces to their concrete data-layer
 * implementations.
 *
 * **Why `@Binds` instead of `@Provides`?**
 * `@Binds` generates more efficient code: Hilt resolves the interface-to-implementation
 * mapping at compile time without generating a factory wrapper method. `@Provides` would
 * require an extra method body and an additional object allocation per injection site.
 *
 * **Why an abstract class?**
 * `@Binds` methods must be abstract (the body is provided by Hilt's code generation).
 * An `abstract class` is used instead of an `interface` so that `@Module` and
 * `@InstallIn` can coexist on the same declaration.
 *
 * **Why [SingletonComponent]?**
 * All repositories are singletons (`@Singleton`) so their internal [kotlinx.coroutines.flow.StateFlow]
 * and Room observer state is shared across the entire application. Installing the module
 * in [SingletonComponent] ensures the bindings are resolved at app startup and reused
 * without re-creation.
 *
 * This module enforces the Clean Architecture contract: the domain layer declares the
 * interfaces; the data layer provides the implementations; Hilt wires them together
 * here without either layer knowing about the other.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [AuthRepositoryImpl] as the single implementation of [AuthRepository].
     *
     * All ViewModels and repositories that need to read or modify the authenticated
     * session inject [AuthRepository] — never the concrete class — preserving the
     * domain layer's independence from the data layer.
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [AuthRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Binds [PatientRepositoryImpl] as the single implementation of [PatientRepository].
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [PatientRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindPatientRepository(impl: PatientRepositoryImpl): PatientRepository

    /**
     * Binds [ConsultationRepositoryImpl] as the single implementation of
     * [ConsultationRepository].
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [ConsultationRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindConsultationRepository(impl: ConsultationRepositoryImpl): ConsultationRepository

    /**
     * Binds [UserRepositoryImpl] as the single implementation of [UserRepository].
     *
     * [UserRepository] is used exclusively by ADMIN-role screens; binding it here
     * still satisfies the Clean Architecture rule that use cases and ViewModels depend
     * only on the domain interface.
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [UserRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    /**
     * Binds [ReportRepositoryImpl] as the single implementation of [ReportRepository].
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [ReportRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    /**
     * Binds [EcgRepositoryImpl] as the single implementation of [EcgRepository].
     *
     * @param impl The concrete implementation constructed and injected by Hilt.
     * @return The bound [EcgRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindEcgRepository(impl: EcgRepositoryImpl): EcgRepository
}
