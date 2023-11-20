package de.memathze.bing.crawler.pages.weather

import de.memathze.bing.crawler.core.DriverSetup
import de.memathze.bing.crawler.core.log
import de.memathze.bing.crawler.pages.Page
import java.time.OffsetDateTime

class BingWeatherPage(driverSetup: DriverSetup) : Page(driverSetup) {

  private val log = log()

  private val eTree by lazy { ETreeModule.forPage(page) }

  fun open() = open(urlForSubPage(WeatherSubPage.WEATHER))

  fun retrievePointsThroughPageVisits() {
    log.info("Start retrieving points through page visits!")
    visitAllStraightPointsPages()
    visitMapPageAndGetPointsFromDifferentViews()
    log.info("Completed retrieving points through page visits!")
  }

  private fun visitAllStraightPointsPages() {
    STRAIGHT_MENU_ACTIONS.forEach {
      val actualScore = eTree.retrieveCurrentScore()
      this.navigateTo(it)
      val newScore = eTree.awaitGotPoints(actualScore)
      log().info("Received {} for visiting {}. Total new score: {}", newScore - actualScore, it, newScore)
    }
  }

  private fun visitMapPageAndGetPointsFromDifferentViews() {
    log.info("Start visiting different map views")
    navigateTo(WeatherSubPage.MAP)
    eTree.awaitETreeBubble()
    var lastPoints = eTree.retrieveCurrentScore()
    val map = MapModule.forPage(page)
    map.switchThroughViews {
      log.info("Await receiving points")
      lastPoints = eTree.awaitGotPoints(lastPoints).also {
        if (it == lastPoints) {
          log.warn("It seems we did not received any points:/")
        } else {
          log.info("We received {} Points. New total: {}", it - lastPoints, it)
        }
      }
    }
  }

  private fun navigateTo(subPage: WeatherSubPage) {
    log.info("Navigate to $subPage")
    open(urlForSubPage(subPage))
  }

  companion object {
    private const val URL = "https://www.msn.com/de-de/wetter/{pathPart}/in-Rostock,Mecklenburg-Vorpommern"
    private val STRAIGHT_MENU_ACTIONS = listOf(
      WeatherSubPage.HOURLY_FORECAST,
      WeatherSubPage.MONTHLY_FORECAST,
      WeatherSubPage.POLLEN,
      WeatherSubPage.HISTORY,
      WeatherSubPage.LIVING,
      WeatherSubPage.WEATHER
    )

    private fun urlForSubPage(subPage: WeatherSubPage) = URL.replace("{pathPart}", subPage.pathPart)

    fun retrieveTimeBasedPoints(driverSetup: DriverSetup) {
      val now = OffsetDateTime.now()
      listOf(8, 12, 16).forEach {
        baseTime = now.withHour(it)
        BingWeatherPage(driverSetup).apply {
          open()
          val actualScore = eTree.retrieveCurrentScore()
          eTree.getActiveTimeBasedBonus()?.run {
            click()
            val newScore = eTree.awaitGotPoints(actualScore)
            log.info("Clicked active bubble and got {}Pts. New score is {}.", newScore - actualScore, newScore)
          } ?: log.info("No active bubble found! Actual score is {}", actualScore)
          close()
        }
      }
    }
  }
}
