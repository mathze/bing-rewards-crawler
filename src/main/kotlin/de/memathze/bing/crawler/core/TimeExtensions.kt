package de.memathze.bing.crawler.core

import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit

fun Duration.toMillis() = this.toDouble(DurationUnit.MILLISECONDS)

fun Duration.sleep() = TimeUnit.NANOSECONDS.sleep(this.inWholeNanoseconds)
