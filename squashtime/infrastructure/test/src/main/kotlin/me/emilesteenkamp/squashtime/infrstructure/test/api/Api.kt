package me.emilesteenkamp.squashtime.infrstructure.test.api

import kotlinx.coroutines.test.runTest
import me.emilesteenkamp.squashtime.infrstructure.test.TestInfrastructure
import me.emilesteenkamp.squashtime.infrstructure.test.create

fun runWithTestInfrastructure(testBlock: suspend TestInfrastructure.() -> Unit) =
    runTest {
        TestInfrastructure::class.create(this).testBlock()
    }
