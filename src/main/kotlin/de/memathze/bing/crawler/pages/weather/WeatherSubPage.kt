package de.memathze.bing.crawler.pages.weather

enum class WeatherSubPage(val pathPart: String) {
  WEATHER("vorhersage"), // should be last
  MAP("karten"), // has sub-actions
  MAP_3D("dummy"), // no points atm
  HOURLY_FORECAST("stundlichevorhersage"),
  MONTHLY_FORECAST("monatlicheprognose"),
  ALERTS("dummy"), // can be get through #MAP
  AIRQUALITY("dummy"), // can be get through #MAP
  POLLEN("pollen"),
  HISTORY("historisches"),
  LIVING("leben")
}
