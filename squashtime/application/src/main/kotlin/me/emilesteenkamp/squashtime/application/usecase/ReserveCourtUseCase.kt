package me.emilesteenkamp.squashtime.application.usecase

import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Schedule
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.base.WorkflowUseCase
import me.emilesteenkamp.squashtime.orktestrator.Orktestrator
import me.tatarka.inject.annotations.Inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReserveCourtUseCase
    @Inject
    constructor(
        private val courtReservationPlatform: CourtReservationPlatform,
        private val courtReservationPlatformPasswordLookup: CourtReservationPlatformPasswordLookup,
    ) : WorkflowUseCase<ReserveCourtUseCase.State.Transient, ReserveCourtUseCase.State.Final>() {
        override val orktestrator =
            Orktestrator.define<State.Transient, State.Final> {
                step(
                    IsNumberOfAdditionalPlayersAllowed,
                    collector = { state -> IsNumberOfAdditionalPlayersAllowed.Input(state.additionalPlayerIdentifierSet) },
                    modifier = { state, isNumberOfAdditionalPlayersAllowed ->
                        if (isNumberOfAdditionalPlayersAllowed) state else State.Final.Error.InvalidNumberOfAdditionalPlayers
                    },
                ) { input ->
                    input.additionalPlayerIdentifierSet.size in 1..3
                }

                step(
                    DeterminePassword,
                    collector = { state -> DeterminePassword.Input(state.player) },
                    modifier = { state, output ->
                        when (output) {
                            is DeterminePassword.Output.Success -> state.copy(password = output.password)
                            DeterminePassword.Output.NotFound -> State.Final.Error.PasswordNotFound
                        }
                    },
                ) { input ->
                    val password = courtReservationPlatformPasswordLookup.find(input.player.userName)

                    if (password != null) {
                        DeterminePassword.Output.Success(password = password)
                    } else {
                        DeterminePassword.Output.NotFound
                    }
                }

                step(
                    StartSession,
                    collector = { state -> StartSession.Input(state.player, state.password.requireNotNull()) },
                    modifier = { state, output ->
                        when (output) {
                            is StartSession.Output.Success -> state.copy(session = output.session)
                            StartSession.Output.Failed -> State.Final.Error.AuthenticationFailed
                        }
                    },
                ) { input ->
                    when (val result = courtReservationPlatform.authenticate(input.player, input.password)) {
                        is CourtReservationPlatform.AuthenticateResult.Success -> StartSession.Output.Success(result.session)
                        CourtReservationPlatform.AuthenticateResult.Failed -> StartSession.Output.Failed
                    }
                }

                step(
                    FetchSchedule,
                    collector = { state ->
                        FetchSchedule.Input(
                            session = state.session.requireNotNull(),
                            requestedDate = state.requestedDateTime.toLocalDate(),
                        )
                    },
                    modifier = { state, output ->
                        when (output) {
                            is FetchSchedule.Output.Success -> state.copy(schedule = output.schedule)
                            FetchSchedule.Output.Failed -> State.Final.Error.FailedToFetchSchedule
                        }
                    },
                ) { input ->
                    when (val result = input.session.fetchSchedule(date = input.requestedDate)) {
                        is CourtReservationPlatform.Session.FetchScheduleResult.Success -> FetchSchedule.Output.Success(result.schedule)
                        CourtReservationPlatform.Session.FetchScheduleResult.Failed -> FetchSchedule.Output.Failed
                    }
                }

                step(
                    FindTimeslot,
                    collector = { state ->
                        FindTimeslot.Input(
                            schedule = state.schedule.requireNotNull(),
                            requestedTime = state.requestedDateTime.toLocalTime(),
                        )
                    },
                    modifier = { state, output ->
                        when (output) {
                            is FindTimeslot.Output.Success -> state.copy(courtTimeslot = output.courtTimeslot)
                            FindTimeslot.Output.NotFound -> State.Final.Error.NoTimeslotAvailable
                        }
                    },
                ) { input ->
                    input.schedule
                        .asSequence()
                        .flatMap { (courtIdentifier, timeslotList) ->
                            timeslotList.map { timeslot ->
                                CourtTimeslot(courtIdentifier = courtIdentifier, timeslot = timeslot)
                            }
                        }.firstOrNull { courtTimeslot ->
                            courtTimeslot.timeslot.time.compareTo(input.requestedTime) == 0 &&
                                courtTimeslot.timeslot.status == Timeslot.Status.FREE
                        }?.let {
                            FindTimeslot.Output.Success(courtTimeslot = it)
                        }
                        ?: FindTimeslot.Output.NotFound
                }

                step(
                    ReserveCourt,
                    collector = { state ->
                        ReserveCourt.Input(
                            session = state.session.requireNotNull(),
                            courtTimeslot = state.courtTimeslot.requireNotNull(),
                            additionalPlayerIdentifierSet = state.additionalPlayerIdentifierSet,
                            requestedDate = state.requestedDateTime.toLocalDate(),
                        )
                    },
                    modifier = { _, output ->
                        when (output) {
                            is ReserveCourt.Output.Success ->
                                State.Final.Success(
                                    bookedCourtIdentifier = output.bookedCourtIdentifier,
                                )
                            ReserveCourt.Output.Failed -> State.Final.Error.CourtReservationFailed
                        }
                    },
                ) { input ->
                    when (
                        input.session.reserveCourt(
                            courtIdentifier = input.courtTimeslot.courtIdentifier,
                            timeslot = input.courtTimeslot.timeslot,
                            additionalPlayerIdentifierSet = input.additionalPlayerIdentifierSet,
                            date = input.requestedDate,
                        )
                    ) {
                        CourtReservationPlatform.Session.ReserveCourtResult.Success ->
                            ReserveCourt.Output.Success(bookedCourtIdentifier = input.courtTimeslot.courtIdentifier)
                        CourtReservationPlatform.Session.ReserveCourtResult.Failed -> ReserveCourt.Output.Failed
                    }
                }
            }

        object State {
            data class Transient(
                val player: Player,
                val additionalPlayerIdentifierSet: Set<Player.Identifier>,
                val requestedDateTime: LocalDateTime,
                val password: CourtReservationPlatform.Password? = null,
                val session: CourtReservationPlatform.Session? = null,
                val schedule: Schedule? = null,
                val courtTimeslot: CourtTimeslot? = null,
            ) : Orktestrator.State.Transient

            sealed interface Final : Orktestrator.State.Final {
                data class Success(
                    val bookedCourtIdentifier: CourtIdentifier,
                ) : Final

                sealed interface Error : Final {
                    data object InvalidNumberOfAdditionalPlayers : Error

                    data object PasswordNotFound : Error

                    data object AuthenticationFailed : Error

                    data object FailedToFetchSchedule : Error

                    data object NoTimeslotAvailable : Error

                    data object CourtReservationFailed : Error
                }
            }
        }

        private object IsNumberOfAdditionalPlayersAllowed : Orktestrator.Step<IsNumberOfAdditionalPlayersAllowed.Input, Boolean> {
            data class Input(
                val additionalPlayerIdentifierSet: Set<Player.Identifier>,
            )
        }

        private object DeterminePassword : Orktestrator.Step<DeterminePassword.Input, DeterminePassword.Output> {
            data class Input(
                val player: Player,
            )

            sealed interface Output {
                data class Success(
                    val password: CourtReservationPlatform.Password,
                ) : Output

                data object NotFound : Output
            }
        }

        private object StartSession : Orktestrator.Step<StartSession.Input, StartSession.Output> {
            data class Input(
                val player: Player,
                val password: CourtReservationPlatform.Password,
            )

            sealed interface Output {
                data class Success(
                    val session: CourtReservationPlatform.Session,
                ) : Output

                data object Failed : Output
            }
        }

        private object FetchSchedule : Orktestrator.Step<FetchSchedule.Input, FetchSchedule.Output> {
            data class Input(
                val session: CourtReservationPlatform.Session,
                val requestedDate: LocalDate,
            )

            sealed interface Output {
                data class Success(
                    val schedule: Schedule,
                ) : Output

                data object Failed : Output
            }
        }

        private object FindTimeslot : Orktestrator.Step<FindTimeslot.Input, FindTimeslot.Output> {
            data class Input(
                val schedule: Schedule,
                val requestedTime: LocalTime,
            )

            sealed interface Output {
                data class Success(
                    val courtTimeslot: CourtTimeslot,
                ) : Output

                data object NotFound : Output
            }
        }

        private object ReserveCourt : Orktestrator.Step<ReserveCourt.Input, ReserveCourt.Output> {
            data class Input(
                val session: CourtReservationPlatform.Session,
                val courtTimeslot: CourtTimeslot,
                val additionalPlayerIdentifierSet: Set<Player.Identifier>,
                val requestedDate: LocalDate,
            )

            sealed interface Output {
                data class Success(
                    val bookedCourtIdentifier: CourtIdentifier,
                ) : Output

                data object Failed : Output
            }
        }

        data class CourtTimeslot(
            val courtIdentifier: CourtIdentifier,
            val timeslot: Timeslot,
        )
    }
