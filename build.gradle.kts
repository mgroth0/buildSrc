repositories {
  mavenCentral()
  mavenLocal()
}

println("check another stupid kt version")
println("validations would be an excellent test suite")
println("check git for big files")
println("root git ignore needs to be synced with root files")
println("ESSENTIAL 3: MAKE LASTVERSION.TXT MORE AUTOMATED")
println("FIX GIT REMOVE SUBMOD")
println("FIX GIT CONFIG WHILE REMVING SUBMOD")
println("TRANSFER BUILDSRC TO KBUILD ETC")
println("give kbuild access to auto submodule and have the non-standard .gitignore requirement open the thing in sublime")
println("maven stuff should be published with full source ... though this might not be neccesary because sometimes intelliJ does resolve straight to the src even though thats not what its directly using...")
println("it would be great if kbuild maven repo was just a full thing like a shadowJar repo? Or is that not neccesarry... not sure about this one")
println("maybe I actually can just share submodules between projects with maven local! that seems faster and more stable too. would be cool to have both that and git as separte options")
println("buildSrc is for sloppy peasants. It slows down every single build, adds multiple completely unnesecary additional layers of complexity to the build process, makes syncing code accross projects more complicated, etc etc... and its not even neccesary! I can build all the gradle plugins I need now in standalone projects that perfectly share logic with the rest of my code. starting with kbuild. ... one might argue that buildSrc is a way to be able to configre how i build my project before i buuild them and that this would lock me into certain uild configuratinos. Well:" +
	"1. so what? old build cfgs should normally be able to build new build cfgs, and I have access to all old build cfgs through maven" +
	"2. this is good motivation to make my gradle plugins extensible" +
	"3. if I really need to as a last resort, all I have to do is copy and paste some of the code from kbuild into a temporary buildSrc and edit as needed temporarily. but this should never happen anyway.")

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

  /*last worked: 1653713570208*/
  implementation("matt.KJ:kbuild:+")
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