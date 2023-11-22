package de.memathze.bing.crawler

import com.github.ajalt.clikt.core.*
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.microsoft.playwright.Playwright
import de.memathze.bing.crawler.core.BrowserKind
import de.memathze.bing.crawler.core.DriverSetup
import de.memathze.bing.crawler.pages.etree.ETreePage
import de.memathze.bing.crawler.pages.rewards.RewardsPage
import de.memathze.bing.crawler.pages.weather.BingWeatherPage

class Main(private val playwright: Playwright) : CliktCommand(allowMultipleSubcommands = true) {
  private val browserKind by option("-b", "--browser", help = "Browser to use").enum<BrowserKind>()
    .default(BrowserKind.EDGE)

  override fun run() {
    currentContext.obj = DriverSetup(playwright, browserKind)
  }
}

class Weather : CliktCommand() {

  private val scenarios by option("-s", "-scenarios").enum<Scenario>().multiple(required = true)

  private val setup by requireObject<DriverSetup>()

  override fun run() = scenarios.forEach {
    when (it) {
      Scenario.TIME -> BingWeatherPage.retrieveTimeBasedPoints(setup)
      Scenario.CLICKS -> BingWeatherPage(setup).run {
        open()
        retrievePointsThroughPageVisits()
        close()
      }
    }
  }

  private enum class Scenario {
    TIME,
    CLICKS
  }
}

class ETree : CliktCommand() {
  private val setup by requireObject<DriverSetup>()
  override fun run() = ETreePage(setup).run {
    open()
    receiveDailyPoints()
    close()
  }
}

class Rewards : CliktCommand() {


  private val scenarios by option("-s", "-scenarios").enum<Scenario>().multiple(required = true)
  private val setup by requireObject<DriverSetup>()

  override fun run() = RewardsPage(setup).run {
    scenarios.sortedBy {
      it.ordinal
    }.forEach {
      when (it) {
        Scenario.DAILY_SET -> doDailyChallenges()
        Scenario.OTHER_ACTIVITIES -> doNormalChallenges()
        Scenario.SEARCH -> doDailySearchChallenge()
      }
    }
  }

  private enum class Scenario {
    DAILY_SET,
    OTHER_ACTIVITIES,
    SEARCH
  }
}

fun main(args: Array<String>) {
  Playwright.create().use { playwright ->
    Main(playwright).subcommands(Weather(), Rewards(), ETree()).main(args)
  }
}
