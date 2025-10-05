package me.emilesteenkamp.squashtime.application.usecase

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import me.emilesteenkamp.orktstrator.api.State
import me.emilesteenkamp.orktstrator.api.Step
import me.emilesteenkamp.orktstrator.definition.OrktstratorDefinition
import me.emilesteenkamp.squashtime.application.domain.CourtIdentifier
import me.emilesteenkamp.squashtime.application.domain.Player
import me.emilesteenkamp.squashtime.application.domain.Schedule
import me.emilesteenkamp.squashtime.application.domain.Timeslot
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatform
import me.emilesteenkamp.squashtime.application.port.CourtReservationPlatformPasswordLookup
import me.emilesteenkamp.squashtime.application.usecase.base.OrktstratorUseCase
import me.tatarka.inject.annotations.Inject

class ReserveCourtUseCase
    @Inject
    constructor(
        private val courtReservationPlatform: CourtReservationPlatform,
        private val courtReservationPlatformPasswordLookup: CourtReservationPlatformPasswordLookup,
    ) : OrktstratorUseCase<
        ReserveCourtUseCase.Input,
        ReserveCourtUseCase.Output,
        ReserveCourtUseCase.UseCaseState.Transient,
        ReserveCourtUseCase.UseCaseState.Final
    >() {
    override fun toInitialState(input: Input): UseCaseState.Transient =
        UseCaseState.Transient(
            player = input.player,
            additionalPlayerIdentifierSet = input.additionalPlayerIdentifierSet,
            requestedDateTime = input.requestedDateTime
        )

    override fun OrktstratorDefinition<UseCaseState.Transient, UseCaseState.Final>.definition() {
        step(
            IsNumberOfAdditionalPlayersAllowed,
            collector = { state -> IsNumberOfAdditionalPlayersAllowed.Input(state.additionalPlayerIdentifierSet) },
            modifier = { state, isNumberOfAdditionalPlayersAllowed ->
                if (isNumberOfAdditionalPlayersAllowed) state else UseCaseState.Final.Error.InvalidNumberOfAdditionalPlayers
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
                    DeterminePassword.Output.NotFound -> UseCaseState.Final.Error.PasswordNotFound
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
                    StartSession.Output.Failed -> UseCaseState.Final.Error.AuthenticationFailed
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
                    FetchSchedule.Output.Failed -> UseCaseState.Final.Error.FailedToFetchSchedule
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
                    FindTimeslot.Output.NotFound -> UseCaseState.Final.Error.NoTimeslotAvailable
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
                        UseCaseState.Final.Success(
                            bookedCourtIdentifier = output.bookedCourtIdentifier,
                        )
                    ReserveCourt.Output.Failed -> UseCaseState.Final.Error.CourtReservationFailed
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

    override fun toOutput(finalisedState: UseCaseState.Final): Output = when (finalisedState) {
        is UseCaseState.Final.Success -> Output(bookedCourtIdentifier = finalisedState.bookedCourtIdentifier)
        UseCaseState.Final.Error.AuthenticationFailed -> error("Court reservation platform authentication failed.")
        UseCaseState.Final.Error.CourtReservationFailed -> error("Court reservation failed.")
        UseCaseState.Final.Error.FailedToFetchSchedule -> error("Failed to fetch schedule.")
        UseCaseState.Final.Error.InvalidNumberOfAdditionalPlayers -> error("Invalid number of additional players.")
        UseCaseState.Final.Error.NoTimeslotAvailable -> error("No timeslot available.")
        UseCaseState.Final.Error.PasswordNotFound -> error("Password not found.")
    }

    data class Input(
        val player: Player,
        val additionalPlayerIdentifierSet: Set<Player.Identifier>,
        val requestedDateTime: LocalDateTime,
    )

    data class Output(
        val bookedCourtIdentifier: CourtIdentifier
    )

    object UseCaseState {
            data class Transient(
                val player: Player,
                val additionalPlayerIdentifierSet: Set<Player.Identifier>,
                val requestedDateTime: LocalDateTime,
                val password: CourtReservationPlatform.Password? = null,
                val session: CourtReservationPlatform.Session? = null,
                val schedule: Schedule? = null,
                val courtTimeslot: CourtTimeslot? = null,
            ) : State.Transient

            sealed interface Final : State.Final {
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

        private object IsNumberOfAdditionalPlayersAllowed : Step<IsNumberOfAdditionalPlayersAllowed.Input, Boolean> {
            data class Input(
                val additionalPlayerIdentifierSet: Set<Player.Identifier>,
            )
        }

        private object DeterminePassword : Step<DeterminePassword.Input, DeterminePassword.Output> {
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

        private object StartSession : Step<StartSession.Input, StartSession.Output> {
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

        private object FetchSchedule : Step<FetchSchedule.Input, FetchSchedule.Output> {
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

        object FindTimeslot : Step<FindTimeslot.Input, FindTimeslot.Output> {
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

        private object ReserveCourt : Step<ReserveCourt.Input, ReserveCourt.Output> {
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
