package de.menkalian.draco.data.game.state

enum class RoundState(val behaviour: BehaviourState) {
    LOBBY(BehaviourState.LOBBY),
    PRE_ROUND(BehaviourState.WAITING),
    GUESSING(BehaviourState.USER_INPUT),
    PRE_FLOP(BehaviourState.USER_INPUT),
    FLOP(BehaviourState.USER_INPUT),
    TURN(BehaviourState.USER_INPUT),
    RIVER(BehaviourState.USER_INPUT),
    RESULTS(BehaviourState.WAITING),
}