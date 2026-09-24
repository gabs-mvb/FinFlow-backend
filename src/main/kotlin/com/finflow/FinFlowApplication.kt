package com.finflow

import com.finflow.FinFlowApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FinFlowApplication

fun main(args: Array<String>) {
    runApplication<FinFlowApplication>(*args)
}
