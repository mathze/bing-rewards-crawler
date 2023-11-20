package de.memathze.bing.crawler.pages.weather

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import de.memathze.bing.crawler.core.log

class MapModule private constructor(root: Locator) {

  private val viewSwitchContainer by lazy { root.locator("#m-switcher") }

  fun switchThroughViews(awaitCompleteAction: () -> Unit) {
    ViewSwitch.entries.forEach {
      log().info("Switching to map view {}", it)
      val switch = findViewSwitchFor(it)
      switch.click()
      awaitCompleteAction()
    }
  }

  private fun findViewSwitchFor(switch: ViewSwitch) = viewSwitchContainer.locator(" *:has(> ${switch.selector})")

  private enum class ViewSwitch(val selector: String) {
    TEMPERATURE(".g_temp-group-button"),
    RAINFALL(".g_precip-group-button"),
    RADAR(".g_radar-group-button"),
    WIND(".g_wind-group-button"),
    CLOUDS(".g_cloud-group-button"),
    HUMIDITY(".g_humidity-group-button"),
    VISIBILITY(".g_visibility-group-button"),
    PRESSURE(".g_pressure-group-button"),
    DEW_POINT(".g_dewpoint-group-button"),
    AIR_QUALITY(".g_air_quality-group-button"),
    HURRICANES(".g_hurricane-group-button"),
    WINTER_WEATHER(".g_winter_storm-group-button"),
    SEVERE_WEATHER(".g_severe_wea-group-button"),
    LIGHTNING(".g_lightning-group-button"),
    POLLEN(".g_pollen_idx-group-button"),
    ETREES(".g_etree-group-button")
  }

  companion object {
    fun forPage(page: Page) = MapModule(page.locator("#map-layout-root"))
  }
}
