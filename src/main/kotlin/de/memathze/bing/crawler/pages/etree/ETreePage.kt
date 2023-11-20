package de.memathze.bing.crawler.pages.etree

import com.microsoft.playwright.Locator
import com.microsoft.playwright.TimeoutError
import de.memathze.bing.crawler.core.DriverSetup
import de.memathze.bing.crawler.core.exists
import de.memathze.bing.crawler.core.hasByLocatorFilter
import de.memathze.bing.crawler.core.hasNotByLocator
import de.memathze.bing.crawler.core.hasNotByLocatorFilter
import de.memathze.bing.crawler.core.log
import de.memathze.bing.crawler.pages.Page
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import com.microsoft.playwright.Page as PlaywrightPage

class ETreePage(driverSetup: DriverSetup) : Page(driverSetup) {

  private val root by lazy { page.locator("#root") }
  private val rootFrame by lazy { root.frameLocator("#untrusted-iframe") }

  private val dailyRewardBtn by lazy { rootFrame.locator("#e-tree-daily-reward-close-button") }

  private val eTreeBtnPanel by lazy { rootFrame.locator("#e-tree-watering-btn-anchor") }
  private val waterBucketBtn by lazy { eTreeBtnPanel.locator("[role='button']:has(.bucket_tip)") }
  private val waterPointsPanel by lazy {
    waterBucketBtn.locator("> *")
      .filter(page.hasByLocatorFilter("[aria-hidden='true']"))
      .filter(page.hasNotByLocatorFilter("[class='bucket_tip']"))
  }

  private val taskListPanel by lazy { rootFrame.locator("#e-tree-task-board-id") }
  private val taskListHeader by lazy { taskListPanel.locator("#e-tree-task-board-anchor") }

  private val taskListItemsPanel by lazy {
    taskListPanel.locator("*")
      .filter(page.hasNotByLocatorFilter("#e-tree-task-board-anchor"))
  }
  private val taskListItems by lazy {
    taskListItemsPanel.locator("*[aria-hidden='false']")
      .locator("> *:not(:empty)")
  }

  fun open() = open(URL)

  fun receiveDailyPoints() {
    // 1. get daily bonus
    catchDailyReward()
    // 2. do daily tasks
    doDailyTasks()
    // 3. give all water
    giveTreeAllWater()
  }

  private fun catchDailyReward() {
    if (dailyRewardBtn.isVisible) {
      log().info("Getting daily reward")
      dailyRewardBtn.click()
    }
  }


  private fun doDailyTasks() {
    // 0. check if tasks left
    var currentTaskStatus = retrieveCurrentTaskStatus()
    if (currentTaskStatus.isInvalid()) {
      log().info("⚠️ Something went wrong while retrieving current task status!")
      return
    }

    if (currentTaskStatus.done == currentTaskStatus.total) {
      log().info("👍All daily tasks ({}/{}) done", currentTaskStatus.done, currentTaskStatus.total)
      return
    }

    // 1. open task-bar and get open tasks
    openTaskBar()
    val openTasks = findAllOpenTasks().also {
      log().debug("Found {} open tasks", it.size)
    }

    // 2. do normal visit tasks
    openTasks.filter { it.isVisitTask() }.forEach {
      currentTaskStatus = performVisitPageTask(it, currentTaskStatus)
    }

    // 3. check if we have a Reader and a Listen task (can be done together)
    val hasReadTask = openTasks.any { it.isReadTask() }
    val hasListeningTask = openTasks.any { it.isListenTask() }
    handleReadAndListeningTasks(hasReadTask, hasListeningTask, currentTaskStatus)

    // 4. close task bar to do not hide water all button
    closeTaskBar()
  }

  private fun retrieveCurrentTaskStatus(): TaskStatus {
    val taskStatusText =
      taskListHeader.locator("> *", Locator.LocatorOptions().setHasText(TaskStatus.REGEX.toPattern())).textContent()
    return TaskStatus.parse(taskStatusText)
  }

  private fun openTaskBar() {
    if (!isTaskBarOpen()) {
      taskListHeader.click()
    }
  }

  private fun closeTaskBar() {
    if (isTaskBarOpen()) {
      taskListHeader.click()
    }
  }

  private fun isTaskBarOpen() = rootFrame.locator("[aria-live='assertive']").all().isNotEmpty()

  private fun findAllOpenTasks(): List<TaskItem> = taskListItems.all().map { TaskItem.create(it) }.filter { it.isOpen() }

  private data class TaskItem(val heading: String, val points: Int, val task: String, val btn: Locator) {

    fun isReadTask() = task.run {
      contains("F9") && contains("5 seconds")
    }

    fun isListenTask() = task.run {
      contains("+U") && contains("Shift+") && contains("10 seconds")
    }

    fun isVisitTask() = !isReadTask() && !isListenTask()

    fun isOpen() = btn.exists()

    companion object {
      private val POINTS_PATTERN = "\\+\\d".toPattern()
      private val BUTTON_PATTERN = "^Go$".toPattern()

      fun create(itemContainer: Locator): TaskItem {
        // should yield 2 'rows'
        val rows = itemContainer.locator("> *")

        val rowWithHeadingAndPoints = rows.filter(Locator.FilterOptions().setHasText(POINTS_PATTERN))
        val headingLoc = rowWithHeadingAndPoints.locator("*", Locator.LocatorOptions().setHasNotText(POINTS_PATTERN))
        val pointsLoc = rowWithHeadingAndPoints.getByText(POINTS_PATTERN)

        val rowWithTaskAndButton = rows.filter(Locator.FilterOptions().setHasNotText(POINTS_PATTERN))
        // either no button nor svg
        val taskLoc = rowWithTaskAndButton.locator("> *", rowWithTaskAndButton.hasNotByLocator("svg")
            .setHasNotText(BUTTON_PATTERN)
        )
        val btnLoc = rowWithTaskAndButton.getByText(BUTTON_PATTERN)

        return TaskItem(
          heading = headingLoc.textContent(),
          points = pointsLoc.textContent().toIntOrNull() ?: 0,
          task = taskLoc.textContent(),
          btn = btnLoc
        )
      }
    }
  }

  private fun performVisitPageTask(task: TaskItem, lastTaskStatus: TaskStatus): TaskStatus {
    log().info("Starting task: {}", task.heading)
    var newPage: PlaywrightPage? = null
    val onPageClbck = Consumer<PlaywrightPage> { t -> newPage = t }

    page.context().onPage(onPageClbck)
    task.btn.click()
    newPage?.waitForLoadState()
    page.context().offPage(onPageClbck)

    try {
      page.waitForCondition {
        val currentStatus = retrieveCurrentTaskStatus()
        currentStatus.done > lastTaskStatus.done
      }
    } catch (_: TimeoutError) {
      log().debug("Might not got the points!")
    }
    newPage?.close()
    return retrieveCurrentTaskStatus()
  }

  private fun handleReadAndListeningTasks(
    hasReadTask: Boolean,
    hasListeningTask: Boolean,
    lastTaskStatus: TaskStatus
  ): TaskStatus {
    if (!hasReadTask && !hasListeningTask) {
      log().info("Nothing to do. No read nor listening tasks.")
      return lastTaskStatus
    }
    // at least one task present, open wiki page (its safer because no cookie-stuff etc.)
    val wiki = page.context().newPage()
    var currentTaskStatus = lastTaskStatus
    wiki.navigate("https://de.wikipedia.org/wiki/Nachhaltigkeit")
    if (hasReadTask) {
      currentTaskStatus = performReaderTask(wiki, currentTaskStatus)
    }
    if (hasListeningTask) {
      currentTaskStatus = performListenerTask(wiki, currentTaskStatus)
    }

    wiki.close()
    return currentTaskStatus
  }

  private fun performReaderTask(targetPage: PlaywrightPage, lastTaskStatus: TaskStatus): TaskStatus {
    log().debug("Perform read task")
    val currentUrl = targetPage.url()
    val readUrl = toReaderUrl(currentUrl)
    targetPage.navigate(readUrl)
    return awaitTaskCompleted(lastTaskStatus).also {
      // reset origin page url
      targetPage.navigate(currentUrl)
    }
  }

  private fun performListenerTask(targetPage: PlaywrightPage, lastTaskStatus: TaskStatus): TaskStatus {
    log().debug("Perform listening task")
    val currentUrl = targetPage.url()
    // because pressing Cntr+Shift+U did not work, we also open the url in read-mode
    val readUrl = toReaderUrl(currentUrl)
    targetPage.navigate(readUrl)
    // then we hit the read aloud button
    val readerBtn = targetPage.locator("#reading-bar-container #readAloudButton")
    readerBtn.click()
    return awaitTaskCompleted(lastTaskStatus).also {
      // reset origin page url
      targetPage.navigate(currentUrl)
    }
  }

  private fun toReaderUrl(url: String) =
    "read://" + url.replace("://", "_") + "?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8.toString())

  private fun awaitTaskCompleted(lastTaskStatus: TaskStatus): TaskStatus {
    var currentStatus = lastTaskStatus
    try {
      page.waitForCondition {
        currentStatus = retrieveCurrentTaskStatus()
        currentStatus.done > lastTaskStatus.done
      }
    } catch (_: TimeoutError) {
      log().debug("Might not got the points!")
    }
    return currentStatus
  }

  private fun giveTreeAllWater() {
    // hit the bucket
    val waterPoints = waterPointsPanel.textContent()?.toIntOrNull() ?: 0
    if (0 < waterPoints) {
      log().info("Going to give {} pts of water", waterPoints)
      waterBucketBtn.click()
    } else log().info("You have no water points")
  }

  companion object {
    private const val URL = "edge://etree/"
  }

  data class TaskStatus(val done: Byte, val total: Byte) {

    fun isInvalid(): Boolean = this === INVALID

    companion object {
      internal const val REGEX = "(?<done>\\d+)/(?<total>\\d+).*"
      private val INVALID = TaskStatus(Byte.MIN_VALUE, Byte.MAX_VALUE)
      fun parse(value: String): TaskStatus = REGEX.toRegex().matchEntire(value)?.run {
        val done = groups["done"]?.value?.toByteOrNull() ?: -1
        val total = groups["total"]?.value?.toByteOrNull() ?: -1
        if (0 > done || 0 > total) INVALID
        else TaskStatus(done, total)
      } ?: INVALID
    }
  }
}
