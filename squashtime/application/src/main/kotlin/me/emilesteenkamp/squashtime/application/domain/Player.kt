package me.emilesteenkamp.squashtime.application.domain

data class Player(
    val identifier: Identifier,
    val userName: UserName
) {
    @JvmInline
    value class Identifier(val value: String)

    @JvmInline
    value class UserName(val value: String)
}