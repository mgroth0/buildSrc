@file:Suppress("PackageDirectoryMismatch")
/*
avoid package statement
so buildscripts dont need to use import statements????
see https://github.com/gradle/gradle/issues/7557
*/

import matt.kbuild.FLOW_FOLDER
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.gradle.api.Action
import org.gradle.api.Project
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
  Toml.parse(FLOW_FOLDER.resolve("RootFiles").resolve("libs.versions.toml").toPath()).getTable("versions")!!
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

fun withTimer(name: String, quiet: Boolean = false, op: ()->Unit) {
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
	if (!quiet) println("$name took ${diff/1000.0} seconds success=${!failed}")
  }


}


val gitSubmodules
  get() = File(".gitmodules")
	.readText()
	.lines()
	.filter { it.startsWith("[") }
	.map { it.substringAfter("\"").substringBefore("\"") }
	.map { it.replace("/", "_") }
	.map {
	  it to File(".gitmodules")
		.readText().substringAfter("\"${it}\"").substringAfter("path =").substringBefore("\n").trim()
	}


val Project.gitFolder get() = projectDir.listFiles()!!.first { it.name == ".git" }
val Project.isGitProject get() = ".git" in projectDir.list()!!

fun Project.execGitFor(task: Exec) = ExecGit(task = task, dir = this.gitFolder.absolutePath)
fun File.execGitFor(task: Exec) = takeIf { this.isDirectory && ".git" in this.list()!! }!!.let { f ->
  ExecGit(
	task = task, dir = f.resolve(".git").absolutePath
  )
}

private val simpleGits = mutableMapOf<Project, SimpleGit>()
val Project.simpleGit: SimpleGit
  get() {
	return simpleGits[this] ?: SimpleGit(gitDir = this.gitFolder.absolutePath).also { simpleGits[this] = it }
  }

fun Exec.gitCommandLine(vararg c: String) {
  if (thisMachine == WINDOWS) {
	commandLine(
	  "C:\\Program Files\\Git\\bin\\sh.exe",
	  "-c",
	  c.joinToString(" ").replace("\\", "/")
	)
  } else {
	commandLine(*c)
  }
}


fun gitShell(vararg c: String, debug: Boolean = false, workingDir: File? = null): String {
  return if (thisMachine == WINDOWS) {
	shell(
	  "C:\\Program Files\\Git\\bin\\sh.exe",
	  "-c",
	  c.joinToString(" ").replace("\\", "/"),
	  workingDir = workingDir,
	  debug = debug
	)
  } else {
	shell(*c, workingDir = workingDir, debug = debug)
  }
}


abstract class GitProject<R>(val dir: String, val debug: Boolean) {

  val gitProjectDir = File(dir).parentFile

  val gitProjectName by lazy { gitProjectDir.name }

  fun branchCommands() = wrapGitCommand(
	"branch",
  )

  fun branch() = op(branchCommands())

  fun addAllCommand() = wrapGitCommand("add", "-A", quietApplicable = false)
  fun commitCommand() = wrapGitCommand("commit", "-m", "autocommit")
  fun addAll() = op(addAllCommand())
  fun commit() = op(commitCommand())

  val commandStart = arrayOf("git", "--git-dir=${dir}")

  private fun wrapGitCommand(vararg command: String, quietApplicable: Boolean = true): Array<String> {
	return if (thisMachine == WINDOWS) {
	  arrayOf(
		"C:\\Program Files\\Git\\bin\\sh.exe",
		"-c",
		*commandStart,
		command.joinToString(" ").replace("\\", "/"),
		*(if (quietApplicable && !debug) arrayOf("--quiet") else arrayOf())
	  )
	} else arrayOf(*commandStart, *command, *(if (quietApplicable && !debug) arrayOf("--quiet") else arrayOf()))
  }

  abstract fun op(command: Array<String>): R

  fun branchDeleteCommand(branchName: String) =
	arrayOf("branch", "-d", branchName)


  fun branchDelete(branchName: String) = op(branchDeleteCommand(branchName))

  fun branchCreateCommand(branchName: String) =
	wrapGitCommand("branch", branchName)

  fun branchCreate(branchName: String) = op(branchCreateCommand(branchName))

  private fun checkoutCommand(branchName: String) =
	wrapGitCommand("checkout", branchName)


  fun checkoutMaster() = op(checkoutCommand("master"))

  fun mergeCommand(branchName: String) =
	wrapGitCommand("merge", branchName)

  fun merge(branchName: String) = op(mergeCommand(branchName))

  fun pushCommand() =
	wrapGitCommand("push", "origin", "master")

  fun push() = op(pushCommand())


  /*using --quiet here, which prevents progress from being reported to stdErr, which was showing up in gradle like:
  *
  *
  *
  *
  From https://github.com/mgroth0/play
 branch            master     -> FETCH_HEAD
  *
  *
  * (so ugly)
  *
  * */
  fun pullCommand() =
	wrapGitCommand("pull", "origin", "master")

  fun pull() = op(pullCommand())
}

class SimpleGit(gitDir: String, debug: Boolean = false): GitProject<String>(gitDir, debug) {
  constructor(projectDir: File, debug: Boolean = false): this(
	projectDir.resolve(".git").absolutePath,
	debug
  )

  override fun op(command: Array<String>): String {
	return shell(*command, debug = debug, workingDir = gitProjectDir)
  }

  private fun isDetatched() = "detatched" in branch()

  private fun reattatch() {
	println("${gitProjectName} is detached! dealing")
	addAll()
	commit()
	branchDelete("tmp")
	branchCreate("tmp")
	checkoutMaster()
	merge("tmp")
	println("dealt with it")
  }

  fun reattatchIfNeeded() {
	if (isDetatched()) reattatch()
  }
}

class ExecGit(val task: Exec, dir: String, debug: Boolean = false): GitProject<Unit>(dir, debug) {
  override fun op(command: Array<String>): Unit {
	task.workingDir(gitProjectDir)
	task.commandLine(*command)
  }
}

fun File.hasParentWithNameStartingWith(s: String): Boolean =
  nameWithoutExtension.startsWith(s) || parentFile?.hasParentWithNameStartingWith(s) ?: false


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