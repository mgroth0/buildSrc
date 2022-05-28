@file:Suppress("PackageDirectoryMismatch")
/*
avoid package statement
so buildscripts dont need to use import statements????
see https://github.com/gradle/gradle/issues/7557
*/

import matt.klib.commons.FLOW_FOLDER
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.tomlj.Toml
import java.io.File

/*SHOULD NEVER USE THIS. DOESNT WORK WITH TOOLING API*/
/*val USER_DIR = File(System.getProperty("user.dir"))*/

fun Project.kotlinCompile(
  cfg: Action<KotlinCompile>
) {
  tasks.withType(KotlinCompile::class.java, cfg)
}


/*obviously this can be improved if needed
I also do this in buildSrc/build.gradle.kts
without calling this. It can't be helped easily.*/
fun tomlVersion(name: String) =
  Toml.parse(FLOW_FOLDER!!.resolve("RootFiles").resolve("libs.versions.toml").toPath()).getTable("versions")!!
	.getString(name)!!


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

fun Exec.setExitHandler(op: (Int, String)->Unit) {
  isIgnoreExitValue = true
  val out = java.io.ByteArrayOutputStream()
  val err = java.io.ByteArrayOutputStream()
  this.standardOutput = out
  this.errorOutput = err

  doLast {
	val result = executionResult.get()
	op(result.exitValue, standardOutput.toString() + errorOutput.toString())
  }

  doLast {
	val stdout = standardOutput.toString()

	if (executionResult.get().exitValue == 0) {
	  //do nothing
	} else if ("nothing to commit" !in stdout) {
	  throw RuntimeException(stdout)
	}
  }
}

fun org.gradle.kotlin.dsl.DependencyHandlerScope.implementations(vararg deps: Any) {
  deps.forEach {
	add("implementation", it)
  }
}