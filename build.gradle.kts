repositories {
  mavenCentral()
}

val rootProj = rootProject
plugins {
  id("groovy")
  `groovy-gradle-plugin`
  `java-gradle-plugin`
  //  File(System.getProperty("user.dir") + "/libs.versions.toml")
  println(File("../libs.versions.toml").canonicalPath)

  /*yes, this is stupidly required because user.dir here is .gradle/daemon or something and plugin block dsl and super weird and restricted. look it up if you don't beleive me.*/
  val stupidKtVersion = "1.6.21"

  kotlin("jvm") version stupidKtVersion

  `kotlin-dsl`/* version "2.1.4"*/
}
val ktversion = rootProject.projectDir.resolve("..").resolve("RootFiles").resolve("libs.versions.toml").readText()
	.lines()
	.first { it.contains("kotlin") }
	.substringAfter("\"")
	.substringBefore("\"")
	.trim()

val thisFile = rootProject.projectDir.resolve("build.gradle.kts")
require(thisFile.readText().substringAfter("stupidKtVersion").substringAfter("\"").substringBefore("\"") == ktversion)

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${ktversion}")
  implementation("org.apache.maven:maven-artifact:3.8.1")
  implementation("org.tomlj:tomlj:1.0.0")
  /*gradleKotlinDsl()*/
  implementation(project("kbuild"))
}

/*

even if I don't use the plugin descriptor, this should disable the warning:
"No valid plugin descriptors were found in META-INF/gradle-plugins"

*/
gradlePlugin {
  val greeting by plugins.creating {
	id = "matt.jbuild.greeting"
	implementationClass = "matt.jbuild.greeting.JGreetingPlugin"
  }
}

