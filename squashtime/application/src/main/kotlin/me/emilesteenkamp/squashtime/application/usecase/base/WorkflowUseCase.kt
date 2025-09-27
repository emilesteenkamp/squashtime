package me.emilesteenkamp.squashtime.application.usecase.base

import me.emilesteenkamp.squashtime.workflow.Workflow

abstract class WorkflowUseCase<TRANSIENT_STATE, FINALISED_STATE> :
    UseCase<TRANSIENT_STATE, FINALISED_STATE>
    where TRANSIENT_STATE : Workflow.State.Transient,
          FINALISED_STATE : Workflow.State.Final {
    protected abstract val workflow: Workflow<TRANSIENT_STATE, FINALISED_STATE>

    override suspend operator fun invoke(input: TRANSIENT_STATE): FINALISED_STATE = workflow.start(input)
}
