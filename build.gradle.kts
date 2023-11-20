plugins {
  kotlin("jvm") version "1.9.0"
  application
}

dependencies {
  implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.4.11")
  implementation(group = "com.microsoft.playwright", name = "playwright", version = "1.37.0")
  implementation(group = "org.awaitility", name = "awaitility", version = "4.2.0")
  implementation(group = "com.github.ajalt.clikt", name = "clikt", version = "4.2.0")
}

application {
  mainClass.set("de.memathze.bing.crawler.MainKt")
}

tasks {
  register<JavaExec>("playwright") {
    group = "exec"
    mainClass.set("de.memathze.bing.crawler.MainKt")
    classpath(sourceSets.main.get().runtimeClasspath)
  }

  register<JavaExec>("dbgPlaywright") {
    group = "exec"
    mainClass.set("de.memathze.bing.crawler.MainKt")
    classpath(sourceSets.main.get().runtimeClasspath)
    environment(
      "PWDEBUG" to "console",
      "PLAYWRIGHT_JAVA_SRC" to sourceSets.main.get().allSource.joinToString(File.pathSeparator)
    )
  }
}
