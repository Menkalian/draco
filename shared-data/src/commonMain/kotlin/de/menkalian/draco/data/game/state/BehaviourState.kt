package de.menkalian.draco.data.game.state

enum class BehaviourState(val joinable: Boolean, val timeoutActive: Boolean) {
    LOBBY(true, false),
    WAITING(false, false),
    USER_INPUT(false, true)
}