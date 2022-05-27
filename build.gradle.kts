repositories {
  mavenCentral()
  mavenLocal()
}


plugins {
  id("groovy")
  `groovy-gradle-plugin`
  `java-gradle-plugin`


  /*yes, this is stupidly required because user.dir here is .gradle/daemon or something and plugin block dsl and super weird and restricted. look it up if you don't beleive me.*/
  val stupidKtVersion = "1.7.0-RC" // "1.6.21"

  kotlin("jvm") version stupidKtVersion

  `kotlin-dsl`
}


fun stupidTomlVersion(name: String) =
  rootProject.projectDir.resolve("..").resolve("RootFiles").resolve("libs.versions.toml").readText()
	.lines()
	.first { it.substringBefore("=").trim() == name }
	.substringAfter("\"")
	.substringBefore("\"")
	.trim()


val verbose = false
if (verbose) {
  println("buildSrc Gradle Version: ${GradleVersion.current()}")
  println("buildSrc JDK Version: ${JavaVersion.current()}")
}

val ktversion = stupidTomlVersion("kotlin")

val thisFile = rootProject.projectDir.resolve("build.gradle.kts")
require(thisFile.readText().substringAfter("stupidKtVersion").substringAfter("\"").substringBefore("\"") == ktversion)

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${ktversion}")
  implementation("org.apache.maven:maven-artifact:3.8.1")
  implementation("org.tomlj:tomlj:1.0.0")


  println("gotta eliminate flow/kcomp from group names so they arent project-specific")

  println("NEED MUCH BETTER DEPENDENCY RESOLUTION HERE BECAUSE IM GETTING INCOMPATIBLE VERSIONS")

  implementation("flow.KJ:kbuild:+")
  implementation("flow.k:klib:+")
  implementation("flow.KJ.kjlib:lang:+")
  implementation("flow.KJ.kjlib:stream:+")
  implementation("flow.KJ.kjlib:socket:+")

}

/*

even if I don't use the plugin descriptor, this should disable the warning:
"No valid plugin descriptors were found in META-INF/gradle-plugins"

*/
//gradlePlugin {
//  val greeting by plugins.creating {
//	id = "matt.jbuild.greeting"
//	implementationClass = "matt.jbuild.greeting.JGreetingPlugin"
//  }
//}


/*
tasks.withType<JavaCompile> {
  this.options.compilerArgs.add("-Xlint:deprecation")
}*/
val javaLangVersion = JavaLanguageVersion.of(stupidTomlVersion("java"))
val javaVersion = JavaVersion.toVersion(stupidTomlVersion("java"))
//allprojects {
//  apply<JavaPlugin>()
//  configure<JavaPluginExtension> {
//	toolchain {
//	  languageVersion.set(javaLangVersion)
//	}
//  }
//  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//	kotlinOptions {
//	  jvmTarget = stupidTomlVersion("java")
//	  languageVersion = stupidTomlVersion("ktlang")
//	}
//  }
//}