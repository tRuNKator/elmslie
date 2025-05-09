package money.vivid.elmslie.core.store

/** Represents result of reduce function */
data class Result<State : Any, Effect : Any, Command : Any>(
  val state: State,
  val effects: List<Effect> = emptyList(),
  val commands: List<Command> = emptyList(),
) {

  constructor(
    state: State,
    effect: Effect?,
    command: Command?,
  ) : this(state = state, effects = listOfNotNull(effect), commands = listOfNotNull(command))
}
