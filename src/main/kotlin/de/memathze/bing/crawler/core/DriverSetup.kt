package de.memathze.bing.crawler.core

import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

data class DriverSetup(
  val driver: Playwright,
  val browser: BrowserKind
) {
  private val context by lazy {
    log().info("Launching new browser context")
    driver.chromium().launchPersistentContext(
      PROFILE, CONTEXT_OPTIONS.setChannel(
        when (browser) {
          BrowserKind.EDGE -> "msedge"
          BrowserKind.CHROME -> "chrome"
        }
      )
    ).also {
      it.onClose { log().info("Closing context") }
    }
  }

  fun newPage(): com.microsoft.playwright.Page = context.newPage()

  companion object {
    private val PROFILE = Path(System.getenv("LOCALAPPDATA"), "Microsoft", "Edge", "User Data", "Default")
    private val CONTEXT_OPTIONS =
      BrowserType.LaunchPersistentContextOptions()
        .setArgs(listOf("--start-fullscreen"))
        .setViewportSize(null)
        .setHeadless(false)
        .setSlowMo(5.seconds.toMillis())
  }
}
