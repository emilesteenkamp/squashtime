package me.emilesteenkamp.squashtime.workflow

class Workflow<TRANSIENT_STATE, FINALISED_STATE>
    private constructor(
        private val graph: Graph<TRANSIENT_STATE, FINALISED_STATE>,
    ) where TRANSIENT_STATE : Workflow.State.Transient,
          FINALISED_STATE : Workflow.State.Final {
    suspend fun start(initialState: TRANSIENT_STATE): FINALISED_STATE {
        val step = graph.initialStep() ?: error("No steps defined.")
        return run(
            state = initialState,
            step = step,
        )
    }

    private suspend fun run(
        state: TRANSIENT_STATE,
        step: Step<*, *>,
    ): FINALISED_STATE {
        val runner = graph.runnerFor(step) ?: error("No step runner for step $step")

        val input =
            try {
                with(runner) {
                    with(CollectorScope) {
                        collector(state)
                    }
                }
            } catch (_: CollectorScope.InvalidWorkflowStateError) {
                error("State in invalid state.")
            }

        val output = runner.performer(input)

        return when (val state = runner.modifier(state, output)) {
            is State.Final -> state.unsafeCast()
            is State.Transient -> {
                val transientState = state.unsafeCast()
                val nextStep = runner.determiner(transientState)
                run(transientState, nextStep)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun State.Final.unsafeCast(): FINALISED_STATE = this as FINALISED_STATE

    @Suppress("UNCHECKED_CAST")
    private fun State.Transient.unsafeCast(): TRANSIENT_STATE = this as TRANSIENT_STATE

    private class Graph<TRANSIENT_STATE, FINALISED_STATE>(
        private val map: LinkedHashMap<Step<*, *>, StepRunner<TRANSIENT_STATE, FINALISED_STATE, *, *>>,
    ) where TRANSIENT_STATE : State.Transient,
              FINALISED_STATE : State.Final {
        fun initialStep(): Step<Any, Any>? =
            map.entries
                .firstOrNull()
                ?.key
                ?.unsafeCast()

        fun runnerFor(step: Step<*, *>): StepRunner<TRANSIENT_STATE, FINALISED_STATE, Any, Any>? = map.get(step)?.unsafeCast()

        @Suppress("UNCHECKED_CAST")
        private fun Step<*, *>.unsafeCast(): Step<Any, Any> = this as Step<Any, Any>

        @Suppress("UNCHECKED_CAST")
        private fun StepRunner<
            TRANSIENT_STATE,
            FINALISED_STATE,
            *,
            *,
        >.unsafeCast(): StepRunner<TRANSIENT_STATE, FINALISED_STATE, Any, Any> =
            this as StepRunner<TRANSIENT_STATE, FINALISED_STATE, Any, Any>
    }

    interface Step<INPUT, OUTPUT> {
        object None : Step<Unit, Unit>
    }

    sealed interface State {
        interface Transient : State

        interface Final : State
    }

    class Builder<TRANSIENT_STATE, FINALISED_STATE>
        internal constructor()
        where TRANSIENT_STATE : State.Transient,
              FINALISED_STATE : State.Final {
        private val graphBuilder = linkedMapOf<Step<*, *>, StepRunnerDefinition<TRANSIENT_STATE, FINALISED_STATE, *, *>>()

        fun <INPUT, OUTPUT> step(
            step: Step<INPUT, OUTPUT>,
            collector: CollectorScope.(TRANSIENT_STATE) -> INPUT,
            modifier: ((TRANSIENT_STATE, OUTPUT) -> State)? = null,
            determiner: ((TRANSIENT_STATE) -> Step<*, *>)? = null,
            performer: suspend (INPUT) -> OUTPUT,
        ) {
            graphBuilder[step] =
                StepRunnerDefinition(
                    collector = collector,
                    modifier = modifier,
                    determiner = determiner,
                    performer = performer,
                )
        }

        @Suppress("UNCHECKED_CAST")
        internal fun build(): Workflow<TRANSIENT_STATE, FINALISED_STATE> {
            val graphMap = linkedMapOf<Step<*, *>, StepRunner<TRANSIENT_STATE, FINALISED_STATE, *, *>>()

            graphBuilder.sequencedEntrySet().forEachIndexed { index, entry ->
                graphMap[entry.key] =
                    StepRunner(
                        collector = entry.value.collector as CollectorScope.(TRANSIENT_STATE) -> Any,
                        modifier = (entry.value.modifier ?: { state, _ -> state }) as (TRANSIENT_STATE, Any) -> State,
                        determiner =
                            entry.value.determiner ?: { _ ->
                                graphBuilder
                                    .sequencedEntrySet()
                                    .elementAtOrNull(index + 1)
                                    ?.key
                                    ?: Step.None
                            },
                        performer = entry.value.performer as suspend (Any) -> Any,
                    )
            }

            return Workflow(Graph(graphMap))
        }

        private data class StepRunnerDefinition<TRANSIENT_STATE, FINALISED_STATE, INPUT, OUTPUT>(
            val collector: CollectorScope.(TRANSIENT_STATE) -> INPUT,
            val modifier: ((TRANSIENT_STATE, OUTPUT) -> State)? = null,
            val determiner: ((TRANSIENT_STATE) -> (Step<*, *>))? = null,
            val performer: suspend (INPUT) -> OUTPUT,
        ) where TRANSIENT_STATE : State.Transient,
                  FINALISED_STATE : State.Final
    }

    private data class StepRunner<TRANSIENT_STATE, FINALISED_STATE, INPUT, OUTPUT>(
        val collector: CollectorScope.(TRANSIENT_STATE) -> INPUT,
        val modifier: (TRANSIENT_STATE, OUTPUT) -> State,
        val determiner: ((TRANSIENT_STATE) -> (Step<*, *>)),
        val performer: suspend (INPUT) -> OUTPUT,
    ) where TRANSIENT_STATE : State.Transient,
              FINALISED_STATE : State.Final

    object CollectorScope {
        fun <T : Any?> T?.requireNotNull(): T = this ?: throw InvalidWorkflowStateError()

        class InvalidWorkflowStateError : Error()
    }

    companion object {
        fun <TRANSIENT_STATE, FINALISED_STATE> define(
            builder: Builder<TRANSIENT_STATE, FINALISED_STATE>.() -> Unit,
        ): Workflow<TRANSIENT_STATE, FINALISED_STATE> where
                                                            TRANSIENT_STATE : State.Transient,
                                                            FINALISED_STATE : State.Final =
            Builder<TRANSIENT_STATE, FINALISED_STATE>().apply(builder).build()
    }
}
