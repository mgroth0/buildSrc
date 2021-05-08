@file:Suppress("PackageDirectoryMismatch")
/*
avoid package statement
so buildscripts dont need to use import statements????
see https://github.com/gradle/gradle/issues/7557
*/

import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.tomlj.Toml
import java.io.File


val USER_DIR = File(System.getProperty("user.dir"))

fun Project.kotlinCompile(
  cfg: Action<KotlinCompile>
) {
  tasks.withType(KotlinCompile::class.java, cfg)
}


/*obviously this can be improved if needed
I also do this in buildSrc/build.gradle.kts
without calling this. It can't be helped easily.*/
fun tomlVersion(name: String) =
	Toml.parse(USER_DIR.resolve("libs.versions.toml").toPath()).getTable("versions")!!.getString(name)!!


val Project.autoReflectionsJar: String
  get() = "matt.reflections:reflections:0.9.13-SNAPSHOT"


fun KotlinJvmOptions.mventionKotlinJvmOptions() {

  jvmTarget = tomlVersion("java")

  languageVersion = tomlVersion("ktlang")

  val aboveKt1_5 = DefaultArtifactVersion(tomlVersion("kotlin")) >= DefaultArtifactVersion("1.5.0")




  this.verbose = true

  this.freeCompilerArgs = mutableListOf<String>(

	/*https://stackoverflow.com/questions/49600947/how-to-suppress-the-requires-transitive-directive-for-an-automatic-module-warn
	*
	* turns out theres a good reason for this warning. I don't think transitive autos even work. Gotta explicitly require them in each until I can convert to a full module or switch to a newer package...
	* */
	//		NOTE:  "-Xlint:-requires-transitive-automatic"
  ) + (if (aboveKt1_5) {


	/*temporary fix for kotlin 1.5.0-RC. Rolls back to old implementation of SAMs. Should be able to remove this within a couple weeks or so. See  https://youtrack.jetbrains.com/issue/KT-45868 and https://youtrack.jetbrains.com/issue/KT-44912*/
	listOf("-Xsam-conversions=class")


  } else listOf())
}

fun withTimer(name: String, op: ()->Unit) {
  var failed = false
  val start = System.currentTimeMillis()
  try {
	op()
  } catch (e: RuntimeException) {
	failed = true
	throw e
  } finally {
	val stop = System.currentTimeMillis()
	val diff = stop - start
	println("$name took ${diff/1000.0} seconds success=${!failed}")
  }


}


val gitSubmodules
  get() = File(".gitmodules")
	  .readText()
	  .lines()
	  .filter { it.startsWith("[") }
	  .map { it.substringAfter("\"").substringBefore("\"") }
	  .map { it.substringBefore("/") }
	  .map {
		it to File(".gitmodules")
			.readText().substringAfter("\"${it}\"").substringAfter("path =").substringBefore("\n").trim()
	  }