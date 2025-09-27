package me.emilesteenkamp.squashtime.infrstructure.test

import kotlin.reflect.KProperty
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope as TestCoroutineScope
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.ReserveCourtUseCase
import me.emilesteenkamp.squashtime.infrstructure.test.mock.MockCourtReservationPlatform
import me.emilesteenkamp.squashtime.infrstructure.test.mock.MockCourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.infrstructure.test.scope.TestScope
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

@TestScope
@Component
abstract class TestInfrastructure(
    @get:Provides
    @Suppress("Unused")
    protected val testScope: TestCoroutineScope
) {
    abstract val reserveCourtUseCase: ReserveCourtUseCase
    abstract val mockCourtReservationPlatformDataSource: MockCourtReservationPlatform.DataSource

    @Provides
    @TestScope
    protected fun mockCourtReservationPlatformDataSource(): MockCourtReservationPlatform.DataSource =
        MockCourtReservationPlatform.DataSource()

    @Provides
    protected fun courtReservationPlatform(dataSource: MockCourtReservationPlatform.DataSource): CourtReservationPlatform = MockCourtReservationPlatform(dataSource)

    @Provides
    protected fun courtReservationPlatformPasswordLookup(): CourtReservationPlatformPasswordLookup =
        MockCourtReservationPlatformPasswordLookup
}
