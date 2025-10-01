package me.emilesteenkamp.squashtime.application.usecase.base

import me.emilesteenkamp.squashtime.orktestrator.Orktestrator

abstract class WorkflowUseCase<TRANSIENT_STATE, FINALISED_STATE> :
    UseCase<TRANSIENT_STATE, FINALISED_STATE>
    where TRANSIENT_STATE : Orktestrator.State.Transient,
          FINALISED_STATE : Orktestrator.State.Final {
    protected abstract val orktestrator: Orktestrator<TRANSIENT_STATE, FINALISED_STATE>

    override suspend operator fun invoke(input: TRANSIENT_STATE): FINALISED_STATE = orktestrator.start(input)
}
