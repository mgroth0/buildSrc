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


  println("NEED MUCH BETTER DEPENDENCY RESOLUTION HERE BECAUSE IM GETTING INCOMPATIBLE VERSIONS")

  /*KEEP THIS LIMITED TO matt.KJ:kbuild, AND LET THAT HAVE API DEPENDENCIES ON ANYTHING ELSE I NEED.
  THEREFORE, I CAN PERMANENT "STABLE" SETS OF BUILDSRC DEPENDENCIES AND NEVER HAVE A CIRCULAR BUILD FAILURE CRISIS AGAIN.

  DO NOT DIRECTELY DEPEND ON ANY OTHER OF MY OWN MODULES OTHER THAN KBUILD.
  ANY OTHER DEPENDENCIES OF MY OWN MODULES SHOULD GO INTO KBUILD IN THE "API" CONFIG.

  thiswill also ensure that builSrc doesnt try to use lower leverl more uptatd libs like the newest klib vefore the higher libs likekjlib have a compatible version. this was a nother bg reason i had issues  before

  IF I HAVE ANY ISSUES, REMOVE THE "+" VERSION BELOW AND FIND THE LAST STABLE VERSION
  */

  /*last worked: 1653680810527*/
  implementation("matt.KJ:kbuild:1653680810527")
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