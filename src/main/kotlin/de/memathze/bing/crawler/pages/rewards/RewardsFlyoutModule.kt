package de.memathze.bing.crawler.pages.rewards

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import de.memathze.bing.crawler.core.Score
import de.memathze.bing.crawler.core.isNull
import de.memathze.bing.crawler.core.toScoreOrNull
import de.memathze.bing.crawler.pages.rewards.RewardsFlyoutModule.ChallengeCard.Companion.NO_CHALLENGE

class RewardsFlyoutModule private constructor(root: Locator) {

  private val dailyChallengeCardsPanel by lazy { root.locator(".flyout_control_threeOffers") }
  private val dailyChallengeCards by lazy { dailyChallengeCardsPanel.locator(".promo_cont") }

  private val normalChallengeCardsPanel by lazy { root.locator(".flyout_control_halfUnit") }
  private val normalChallengeCards by lazy { normalChallengeCardsPanel.locator(".promo_cont") }

  fun findAllOpenDailyChallenges(): List<ChallengeCard> = dailyChallengeCards.all()
      .map { ChallengeCard.createChallengeCard(it) }
      .filterNot { NO_CHALLENGE === it }

  fun findAllOpenNormalChallenges() = normalChallengeCards.all()
    .map { ChallengeCard.createChallengeCard(it) }
    .filterNot { NO_CHALLENGE === it }

  open class ChallengeCard(val title: String, val description: String, val points: Score, val btn: Locator?) {

    override fun equals(other: Any?): Boolean {
      return if (other.isNull()) false
      else if (other is ChallengeCard) other.title == title && other.description == description
      else false
    }

    override fun hashCode(): Int {
      var result = title.hashCode()
      result = 31 * result + description.hashCode()
      return result
    }

    companion object {

      val NO_CHALLENGE = object : ChallengeCard("", "", Score.MIN_VALUE, null) {}

      fun createChallengeCard(cardLocator: Locator): ChallengeCard {
        val points: String? = cardLocator.locator(".point_cont .point").textContent()
        return if (points?.isNotBlank() == true) {
          val challengeTitle = cardLocator.locator(".promo-title").textContent()
          val challengeDescription = cardLocator.locator(".promo-desc").textContent()
          ChallengeCard(
            title = challengeTitle,
            description = challengeDescription,
            points = points.toScoreOrNull() ?: 0,
            btn = cardLocator
          )
        } else {
          NO_CHALLENGE
        }
      }
    }
  }

  companion object {

    private const val REWARD_FLYOUD_FRAME_LOC = "#panelFlyout"
    private const val REWARD_MAIN_PANEL_LOC = "#app"

    fun forPage(page: Page) = RewardsFlyoutModule(
      page.frameLocator(REWARD_FLYOUD_FRAME_LOC)
        .locator(REWARD_MAIN_PANEL_LOC)
    )
  }
}
