package me.emilesteenkamp.squashtime.application.usecase.base

interface UseCase<INPUT, OUTPUT> {
    suspend operator fun invoke(input: INPUT): OUTPUT
}
