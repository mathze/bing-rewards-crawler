package de.memathze.bing.crawler.core

fun <T> T?.isNull() = null == this

typealias Score = Int
internal fun String?.toScoreOrNull(): Score? = this?.toIntOrNull()
