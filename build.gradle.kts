repositories {
  mavenCentral()
}

val rootProj = rootProject
plugins {
  id("groovy")
  `groovy-gradle-plugin`
  `java-gradle-plugin`
  kotlin("jvm") version File(System.getProperty("user.dir") + "/libs.versions.toml").readText()
	  .lines()
	  .first { it.contains("kotlin") }
	  .substringAfter("\"")
	  .substringBefore("\"")
	  .trim()
}
val ktversion = rootProject.projectDir.resolve("../libs.versions.toml").readText()
	.lines()
	.first { it.contains("kotlin") }
	.substringAfter("\"")
	.substringBefore("\"")
	.trim()
dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${ktversion}")
  implementation("org.apache.maven:maven-artifact:3.8.1")
  implementation("org.tomlj:tomlj:1.0.0")
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