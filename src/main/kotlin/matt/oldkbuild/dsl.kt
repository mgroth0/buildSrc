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

/*
* This file is specifically for things that can't go into kbuild because aparently kbuild can not have the kotlin gradle dsl as a dependency?
* */

//fun Project.kotlinCompile(
//  cfg: Action<KotlinCompile>
//) {
//  tasks.withType(KotlinCompile::class.java, cfg)
//}


//val Project.autoReflectionsJar: String
//  get() = "matt.reflections:reflections:0.9.13-SNAPSHOT"


fun KotlinJvmOptions.mventionKotlinJvmOptions(kbuild: Boolean = false) {



  jvmTarget = if (kbuild) "1.8" else matt.kbuild.tomlVersion("java")

  languageVersion = matt.kbuild.tomlVersion("ktlang")

  val aboveKt1_5 = DefaultArtifactVersion(matt.kbuild.tomlVersion("kotlin")) >= DefaultArtifactVersion("1.5.0")




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

fun org.gradle.kotlin.dsl.DependencyHandlerScope.implementations(vararg deps: Any) {
  deps.forEach {
	add("implementation", it)
  }
}