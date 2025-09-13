package me.emilesteenkamp.squashtime.workflow

class Workflow<TRANSIENT_STATE, FINALISED_STATE>
private constructor(
    private val graph: Graph<TRANSIENT_STATE, FINALISED_STATE>
) where TRANSIENT_STATE : Workflow.State.Transient,
        FINALISED_STATE : Workflow.State.Final
{
    suspend fun start(initialState: TRANSIENT_STATE): FINALISED_STATE {
        val step = graph.initialStep() ?: error("No steps defined.")
        return run(
            state = initialState,
            step = step
        )
    }

    private suspend fun run(
        state: TRANSIENT_STATE,
        step: Step<*, *>
    ): FINALISED_STATE {
        val runner = graph.runnerFor(step) ?: error("No step runner for step $step")

        val input = runner.collector(state)
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
        private val linkedHashMap: LinkedHashMap<Step<*, *>, StepRunner<TRANSIENT_STATE, FINALISED_STATE, *, *>>
    ) where TRANSIENT_STATE : State.Transient,
            FINALISED_STATE : State.Final
    {
        fun initialStep(): Step<Any, Any>? =
            linkedHashMap.entries.firstOrNull()?.key?.unsafeCast()

        fun runnerFor(step: Step<*, *>): StepRunner<TRANSIENT_STATE, FINALISED_STATE, Any, Any>? =
            linkedHashMap.get(step)?.unsafeCast()

        @Suppress("UNCHECKED_CAST")
        private fun Step<*, *>.unsafeCast(): Step<Any, Any> =
            this as Step<Any, Any>

        @Suppress("UNCHECKED_CAST")
        private fun StepRunner<TRANSIENT_STATE, FINALISED_STATE, *, *>.unsafeCast(): StepRunner<TRANSIENT_STATE, FINALISED_STATE, Any, Any> =
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
                  FINALISED_STATE : State.Final
    {
        private val graph = linkedMapOf<Step<*, *>, StepRunner<TRANSIENT_STATE, FINALISED_STATE, *, *>>()

        fun <INPUT, OUTPUT, NEXT_INPUT, NEXT_OUTPUT> step(
            step: Step<INPUT, OUTPUT>,
            collector: (TRANSIENT_STATE) -> INPUT,
            modifier: (TRANSIENT_STATE, OUTPUT) -> State,
            determiner: (TRANSIENT_STATE) -> Step<NEXT_INPUT, NEXT_OUTPUT>,
            performer: suspend (INPUT) -> OUTPUT
        ) {
            graph[step] = StepRunner(
                collector = collector,
                modifier = modifier,
                determiner = determiner,
                performer = performer
            )
        }

        internal fun build(): Workflow<TRANSIENT_STATE, FINALISED_STATE> {
            return Workflow(Graph(graph))
        }
    }

    private data class StepRunner<TRANSIENT_STATE, FINALISED_STATE, INPUT, OUTPUT>(
        val collector: (TRANSIENT_STATE) -> INPUT,
        val modifier: (TRANSIENT_STATE, OUTPUT) -> State,
        val determiner: ((TRANSIENT_STATE) -> (Step<*, *>)),
        val performer: suspend (INPUT) -> OUTPUT
    ) where TRANSIENT_STATE : State.Transient,
            FINALISED_STATE : State.Final

    companion object {
        fun <TRANSIENT_STATE, FINALISED_STATE> define(
            builder: Builder<TRANSIENT_STATE, FINALISED_STATE>.() -> Unit
        ): Workflow<TRANSIENT_STATE, FINALISED_STATE> where
                TRANSIENT_STATE : State.Transient,
                FINALISED_STATE : State.Final =
            Builder<TRANSIENT_STATE, FINALISED_STATE>().apply(builder).build()
    }
}
