package me.emilesteenkamp.squashtime.application.usecase.base

import io.github.oshai.kotlinlogging.KotlinLogging
import me.emilesteenkamp.orktstrator.api.Orktstrator
import me.emilesteenkamp.orktstrator.api.State
import me.emilesteenkamp.orktstrator.definition.OrktstratorDefinition
import me.emilesteenkamp.orktstrator.definition.define

abstract class OrktstratorUseCase<INPUT, OUTPUT, TRANSIENT_STATE, FINALISED_STATE> :
    UseCase<INPUT, OUTPUT>
    where TRANSIENT_STATE : State.Transient,
          FINALISED_STATE : State.Final
{
    private val logger = KotlinLogging.logger {}
    private val useCaseName = this::class.simpleName ?: "[UnspecifiedUseCase]"
    private val orktestrator: Orktstrator<TRANSIENT_STATE, FINALISED_STATE> = Orktstrator.define {
        intercept<Any?, Any?> {
            beforeStart { initialState, _ ->
                logger.atInfo {
                    message = "Performing use case $useCaseName"
                    payload = mapOf(
                        "initialState" to initialState
                    )
                }
            }
            onOut { step, input, output ->
                val stepName = step::class.simpleName ?: "[UnspecifiedStep]"
                logger.atInfo {
                    message = "Performed step $stepName"
                    payload = mapOf(
                        "step" to stepName,
                        "input" to input,
                        "output" to output
                    )
                }
            }
            afterCompletion { finalisedState ->
                logger.atInfo {
                    message = "Completed use case $useCaseName"
                    payload = mapOf(
                        "finalisedState" to finalisedState
                    )
                }
            }
        }
        definition()
    }

    abstract fun toInitialState(input: INPUT): TRANSIENT_STATE
    abstract fun OrktstratorDefinition<TRANSIENT_STATE, FINALISED_STATE>.definition()
    abstract fun toOutput(finalisedState: FINALISED_STATE): OUTPUT

    override suspend operator fun invoke(input: INPUT): OUTPUT {
        val initialState = toInitialState(input)
        val finalisedState = orktestrator.orchestrate(initialState)
        return toOutput(finalisedState)
    }
}
