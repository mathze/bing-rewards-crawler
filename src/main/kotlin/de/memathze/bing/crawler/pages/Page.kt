package de.memathze.bing.crawler.pages

import de.memathze.bing.crawler.core.DriverSetup
import java.io.Closeable
import java.time.OffsetDateTime

open class Page(setup: DriverSetup) : Closeable {
  protected val page = setup.newPage()

  protected fun open(url: String) {
    page.apply {
      val script = javaClass.getResource("/timeMock.js")?.run {
        readText().replaceFirst("%{baseTime}", baseTime.toString())
      }
      addInitScript(script ?: "")
    }.navigate(url)
  }

  override fun close() = page.close()

  fun changePageDateTimeTo(newDateTime: OffsetDateTime) {
    page.evaluate("date => MockDate.seed(date)", newDateTime.toString())
  }

  fun resetPageDateTime() {
    page.evaluate("MockDate.reset()")
  }

  companion object {
    var baseTime = OffsetDateTime.now()
  }
}
