package de.memathze.bing.crawler.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T: Any> T.log(): Logger = LoggerFactory.getLogger(T::class.java)
