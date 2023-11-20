package de.memathze.bing.crawler.pages.weather

import com.microsoft.playwright.ElementHandle
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import de.memathze.bing.crawler.core.Score
import de.memathze.bing.crawler.core.awaitElement
import de.memathze.bing.crawler.core.awaitVisible
import de.memathze.bing.crawler.core.log
import de.memathze.bing.crawler.core.toScoreOrNull
import org.awaitility.Awaitility
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class ETreeModule private constructor(
  private val eTreeBubble: Locator,
  private val dailyBubblesRoot: Locator,
  private val eTreeBoard: Locator
) {

  private val activeTimeBasedBonusBubble by lazy { dailyBubblesRoot.locator(ETREE_ACTIVE_DAILY_BUBBLE_LOC) }
  private val boardPanelMenu by lazy { eTreeBoard.locator("[class^='boardMenuContentContainer-DS-'], [class*=' boardMenuContentContainer-DS-']") }
  private val ownRankPanel by lazy { eTreeBoard.locator("[class^='boardRankSelfContainer-DS-'], [class*=' boardRankSelfContainer-DS-']") }

  fun awaitETreeBubble() = eTreeBubble.awaitVisible(maxTimeout = ETREE_BUBBLE_APPEAR_TIMEOUT)

  fun getActiveTimeBasedBonus(): ElementHandle? = activeTimeBasedBonusBubble.awaitElement()

  fun retrieveCurrentScore(): Score {
    log().debug("Getting current score")

    openDetailsPanel()

    switchToScoreBoard()

    // retrieve current score
    val score = ownRankPanel.awaitVisible()
      .locator("[class^='commonText2-DS-'], [class*=' commonText2-DS-']")
      .textContent()
      .toScoreOrNull() ?: 0

    log().debug("Current score is {}", score)
    return score
  }

  private fun openDetailsPanel() {
    if (eTreeBubble.isVisible) {
      log().debug("Clicking eTreeBubble")
      eTreeBubble.click()
      log().debug("Await eTreeBoard to become visible")
      eTreeBoard.awaitVisible()
      log().debug("eTreeBoard visible: {}", eTreeBoard.isVisible)
    }
  }

  private val rankBtn by lazy { boardPanelMenu.locator("[data-value='rank']") }
  private fun switchToScoreBoard() {
    if (!isRankButtonActive()) {
      log().debug("Switching to score board")
      rankBtn.click()
    } else log().debug("Score board already active")
  }

  private fun isRankButtonActive(): Boolean {
    val imageSrc = rankBtn.locator("img").getAttribute("src")
    return !imageSrc.endsWith("Unselected.png")
  }

  fun awaitGotPoints(
    actualScore: Score,
    atMost: Int = 3,
    sleep: Duration = Duration.ofSeconds(1)
  ): Score {
    log().debug("Awaiting score change")
    var latestScore = actualScore
    val cnt = AtomicInteger(atMost)
    Awaitility.with().pollDelay(Duration.ZERO)
      .and().pollInterval(sleep)
      .await().forever().runCatching {
        until {
          latestScore = retrieveCurrentScore()
          log().debug("New score is $latestScore")
          latestScore > actualScore || cnt.decrementAndGet() == 0
        }
      }.onFailure {
        log().debug("Caught exception", it)
      }

    return latestScore
  }

  companion object {
    private val ETREE_BUBBLE_APPEAR_TIMEOUT = 10.seconds

    private const val ETREE_BUBBLE_LOC = "#eplantEntranceBubble"
    private const val ETREE_BOARD_LOC = "#etreeboard"
    private const val ETREE_DAILY_BUBBLE_ROOT_LOC = "*[class*='timingEnergyContainer-DS-EntryPoint1']"
    private const val ETREE_ACTIVE_DAILY_BUBBLE_LOC = "*[class*='energyBubble'][class~='active']"

    fun forPage(page: Page) = ETreeModule(
      page.locator(ETREE_BUBBLE_LOC),
      page.locator(ETREE_DAILY_BUBBLE_ROOT_LOC),
      page.locator(ETREE_BOARD_LOC)
    )
  }
}
