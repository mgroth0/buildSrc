


repositories {
  mavenCentral()
  mavenLocal()
}



val rootProj = rootProject
//rootProj.acc
plugins {
  id("groovy")
  `groovy-gradle-plugin`
  `java-gradle-plugin`

  //  File(System.getProperty("user.dir") + "/libs.versions.toml")
//  println(File("../libs.versions.toml").canonicalPath)

  /*yes, this is stupidly required because user.dir here is .gradle/daemon or something and plugin block dsl and super weird and restricted. look it up if you don't beleive me.*/
  val stupidKtVersion = "1.7.0-RC" // "1.6.21"

  kotlin("jvm") version stupidKtVersion

//  `kotlin-dsl`/* version "2.1.4"*/
}


fun stupidTomlVersion(name: String) =
  rootProject.projectDir.resolve("..").resolve("RootFiles").resolve("libs.versions.toml").readText()
	.lines()
	.first { it.substringBefore("=").trim() == name }
	.substringAfter("\"")
	.substringBefore("\"")
	.trim()

//tasks.wrapper {
//  distributionType = Wrapper.DistributionType.ALL
//  val tomlGradleVersion = stupidTomlVersion("gradle")
//  println("buildSrc wrapper gradle version set to $tomlGradleVersion")
//  gradleVersion = tomlGradleVersion
//}

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
  /*gradleKotlinDsl()*/
  /*implementation(project("kbuild"))*/
  implementation("flow.KJ:kbuild:+")
  implementation("flow.KJ.kjlib:lang:+")
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


/*
tasks.withType<JavaCompile> {
  this.options.compilerArgs.add("-Xlint:deprecation")
}*/
val javaLangVersion = JavaLanguageVersion.of(stupidTomlVersion("java"))
val javaVersion = JavaVersion.toVersion(stupidTomlVersion("java"))
allprojects {
  apply<JavaPlugin>()
  configure<JavaPluginExtension> {
	toolchain {
	  languageVersion.set(javaLangVersion)
	}
  }
  tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
	  jvmTarget = stupidTomlVersion("java")
	  languageVersion = stupidTomlVersion("ktlang")
	}
  }
  if (this.projectDir.name == "kbuild") {
	apply<JavaGradlePluginPlugin>()
	configure<GradlePluginDevelopmentExtension>() {
	  val dummyToAvoidWarning by plugins.creating {
		id = "matt.kbuild.dummy"
		implementationClass = "matt.kbuild.dummy.dummy"
	  }
	}
  }
}