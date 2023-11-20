package de.memathze.bing.crawler.core

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.TimeoutError
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun Page.hasByLocatorFilter(selector: String): Locator.FilterOptions =
  Locator.FilterOptions().setHas(this.locator(selector))

fun Locator.hasByLocatorFilter(selector: String): Locator.FilterOptions =
  Locator.FilterOptions().setHas(page().locator(selector))

fun Locator.hasByLocator(selector: String): Locator.LocatorOptions =
  Locator.LocatorOptions().setHas(page().locator(selector))

fun Page.hasNotByLocatorFilter(selector: String): Locator.FilterOptions =
  Locator.FilterOptions().setHasNot(this.locator(selector))

fun Locator.hasNotByLocatorFilter(selector: String): Locator.FilterOptions =
  Locator.FilterOptions().setHasNot(page().locator(selector))

fun Locator.hasNotByLocator(selector: String): Locator.LocatorOptions =
  Locator.LocatorOptions().setHasNot(page().locator(selector))

fun Locator.exists() = this.count() > 0

fun Locator.awaitElement(timeout: Duration = 1.seconds): ElementHandle? = try {
  log().debug("Awaiting element for {}", this)
  this.elementHandle(Locator.ElementHandleOptions().setTimeout(timeout.toMillis()))
} catch (_: TimeoutError) {
  log().debug("Element not found!")
  null
}

fun Locator.awaitVisible(wait: Duration = 200.milliseconds, maxTimeout: Duration = 5.seconds): Locator {
  log().debug("Awaiting {} to become visible", this)

  try {
    Awaitility.await().atMost(maxTimeout.toJavaDuration())
      .and().pollDelay(wait.toJavaDuration())
      .and().ignoreException(TimeoutError::class.java)
      .until { isVisibleOnScreen(wait) }
  } catch (_: ConditionTimeoutException) {
  }

  return this
}

private const val VISIBILITY_JS = """
  node => {
    const bb = node.getBoundingClientRect();
    const { innerHeight, innerWidth } = window;
    return bb.right >= 0 && bb.right <= innerWidth 
      && bb.left >= 0 && bb.left <= innerWidth
      && bb.top >= 0 && bb.top <= innerHeight
      && bb.bottom >= 0 && bb.bottom <= innerHeight;
  }"""

fun Locator.isVisibleOnScreen(timeout: Duration = 500.milliseconds): Boolean {
  log().debug("Checking visibility of {}", this)
  return isVisible && evaluate(VISIBILITY_JS, null, Locator.EvaluateOptions().setTimeout(timeout.toMillis())) as Boolean
}
