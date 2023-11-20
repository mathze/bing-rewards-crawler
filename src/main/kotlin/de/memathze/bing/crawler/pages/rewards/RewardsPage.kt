package de.memathze.bing.crawler.pages.rewards

import de.memathze.bing.crawler.core.DriverSetup
import de.memathze.bing.crawler.core.Score
import de.memathze.bing.crawler.core.log
import de.memathze.bing.crawler.core.toScoreOrNull
import de.memathze.bing.crawler.pages.Page
import org.awaitility.Awaitility
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

open class RewardsPage(driverSetup: DriverSetup) : Page(driverSetup) {

  private val rewardsMenu = page.locator("#id_rh")
  private val rewardsScore = rewardsMenu.locator("#id_rc")

  private val rewardsModule = RewardsFlyoutModule.forPage(page)
  private val searchInput = page.locator("#sb_form_q")
  private val searchGoInput = page.locator("#sb_search, #search_icon")

  private fun open() = open(URL)

  fun doDailyChallenges() = try {
    open()
    performDailyChallenges()
    close()
  } catch (ex: Exception) {
    log().error("Error in DailyChallenges.", ex)
  }

  fun doNormalChallenges() = try {
    open()
    performNormalChallenges()
    close()
  } catch (ex: Exception) {
    log().error("Error in NormalChallenges.", ex)
  }

  fun doDailySearchChallenge() = try {
    open()
    performDailySearchChallenge()
    close()
  } catch (ex: Exception) {
    log().error("Error in DailySearchChallenge.", ex)
  }

  private fun performDailyChallenges() {
    log().info("Start doing daily challenges")
    var currentScore = getCurrentScore()
    var challenges: List<RewardsFlyoutModule.ChallengeCard>
    val doneChallenges: MutableList<RewardsFlyoutModule.ChallengeCard> = ArrayList()
    while (true) {
      openRewardsBoard()
      challenges = rewardsModule.findAllOpenDailyChallenges()
      if (challenges.isEmpty()) {
        break
      }
      val challenge = challenges.first()
      if (doneChallenges.contains(challenge)) {
        continue
      }
      log().info("Going to do daily challenge '${challenge.title}'")
      challenge.btn?.click()
      if ("Rewards-Umfrage des Tages" == challenge.title) {
        doRewardsDailyQuestion()
      }

      currentScore = awaitScoreChange(currentScore)
      doneChallenges += challenge
    }
    log().info("Did ${doneChallenges.count()} daily challenges")
  }

  fun doRewardsDailyQuestion() {

  }

  private fun performNormalChallenges() {
    log().info("Start doing normal challenges")
    var currentScore = getCurrentScore()
    var challenges: List<RewardsFlyoutModule.ChallengeCard>
    val doneChallenges: MutableList<RewardsFlyoutModule.ChallengeCard> = ArrayList()
    while (true) {
      openRewardsBoard()
      challenges = rewardsModule.findAllOpenNormalChallenges()
      if (challenges.isEmpty()) {
        break
      }
      val challenge = challenges.first()
      if (doneChallenges.contains(challenge)) {
        continue
      }
      log().info("Going to do normal challenge '${challenge.title}'")
      challenge.btn?.click()

      currentScore = awaitScoreChange(currentScore)
      doneChallenges += challenge
    }
    log().info("Did ${doneChallenges.count()} normal challenges")
  }

  private fun performDailySearchChallenge() {
    // get real number of open search (max 30 normal + 4 for edge + 4 as buffer)
    var availableOpenSearches = 30 + 4 + 4

    var actualScore = getCurrentScore()
    log().info("Starting daily search with score {}.", actualScore)
    val searchedTerms = mutableListOf<String>()
    while (availableOpenSearches-- > 0){
      val term = receiveTerm(searchedTerms)
      doSearch(term)
      actualScore = awaitScoreChange(actualScore)
      searchedTerms += term
    }
    log().info("Finished daily search with score {}.", actualScore)
  }

  private fun receiveTerm(searchedTerms: List<String>): String {
    var tries = 5
    var term: String
    do {
      term = SearchDictionary.aRandomTerm()
    } while (searchedTerms.contains(term) && 0 < --tries)
    return term
  }

  private fun doSearch(term: String) {
    searchInput.clear()
    searchInput.type(term)
    searchGoInput.click()
  }

  private fun getCurrentScore() = rewardsScore.textContent().toScoreOrNull() ?: 0

  private fun openRewardsBoard() = rewardsMenu.click()

  private fun awaitScoreChange(actualScore: Score): Score {
    var currentScore = actualScore
    log().debug("Await until current score $actualScore changes")
    val cnt = AtomicInteger(3)
    Awaitility.await("Score update")
      .with().pollDelay(Duration.ZERO)
      .and().pollInterval(Duration.ofSeconds(1))
      .forever()
      .runCatching {
        until {
          currentScore = getCurrentScore()
          currentScore > actualScore|| cnt.decrementAndGet() == 0
        }
      }.onFailure {
        log().debug("Caught exception", it)
      }
    log().debug("New score $currentScore")
    return currentScore
  }

  private companion object {
    const val URL = "https://www.bing.com/"
  }

  private object SearchDictionary {
    val words = javaClass.getResource("/search.dict")?.readText()?.split("(\r)?\n".toRegex()) ?: listOf()
    val rand = Random.Default

    fun aRandomTerm() = rand.nextInt(words.size).let {
      words[it]
    }
  }
}
