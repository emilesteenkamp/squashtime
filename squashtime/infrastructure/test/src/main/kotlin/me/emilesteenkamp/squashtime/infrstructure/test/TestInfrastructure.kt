package me.emilesteenkamp.squashtime.infrstructure.test

import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.peripheral.mock.MockCourtReservationPlatform
import me.emilesteenkamp.squashtime.peripheral.mock.MockCourtReservationPlatformPasswordLookup
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import kotlinx.coroutines.test.TestScope as TestCoroutineScope
import me.emilesteenkamp.squashtime.infrastructure.scope.testscoped.TestScoped

@TestScoped
@Component
abstract class TestInfrastructure(
    @get:Provides
    @Suppress("Unused")
    val testScope: TestCoroutineScope,
) {
    abstract val reserveCourtUseCase: ReserveCourtUseCase
    abstract val mockCourtReservationPlatformDataSource: MockCourtReservationPlatform.DataSource

    internal val MockCourtReservationPlatform.bind: CourtReservationPlatform
        @Provides get() = this

    internal val MockCourtReservationPlatformPasswordLookup.bind: CourtReservationPlatformPasswordLookup
        @Provides get() = this
}
