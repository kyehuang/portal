package com.portal.worker

import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain

@QuarkusMain
class WorkerApplication : QuarkusApplication {
    override fun run(vararg args: String): Int {
        Quarkus.waitForExit()
        return 0
    }
}
