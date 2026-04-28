package dam.pmdm.pqrst.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dam.pmdm.pqrst.data.auth.AuthRepositoryImpl
import dam.pmdm.pqrst.data.repository.ConsultationRepositoryImpl
import dam.pmdm.pqrst.data.repository.PatientRepositoryImpl
import dam.pmdm.pqrst.data.repository.UserRepositoryImpl
import dam.pmdm.pqrst.domain.repository.AuthRepository
import dam.pmdm.pqrst.domain.repository.ConsultationRepository
import dam.pmdm.pqrst.domain.repository.PatientRepository
import dam.pmdm.pqrst.domain.repository.UserRepository
import javax.inject.Singleton

/**
 * Hilt module that binds repository interfaces to their concrete implementations.
 *
 * Using `@Binds` instead of `@Provides` avoids the overhead of an extra factory method;
 * Hilt resolves the binding at compile time.
 *
 * Installed in [SingletonComponent] so all repositories are app-lifetime singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds [AuthRepositoryImpl] as the implementation of [AuthRepository].
     *
     * @param impl The concrete implementation provided by Hilt.
     * @return The bound [AuthRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    /**
     * Binds [PatientRepositoryImpl] as the implementation of [PatientRepository].
     *
     * @param impl The concrete implementation provided by Hilt.
     * @return The bound [PatientRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindPatientRepository(impl: PatientRepositoryImpl): PatientRepository

    /**
     * Binds [ConsultationRepositoryImpl] as the implementation of [ConsultationRepository].
     *
     * @param impl The concrete implementation provided by Hilt.
     * @return The bound [ConsultationRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindConsultationRepository(impl: ConsultationRepositoryImpl): ConsultationRepository

    /**
     * Binds [UserRepositoryImpl] as the implementation of [UserRepository].
     *
     * @param impl The concrete implementation provided by Hilt.
     * @return The bound [UserRepository] interface.
     */
    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository
}
