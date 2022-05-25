@file:Suppress("PackageDirectoryMismatch")


import java.io.File

import matt.kbuild.recurse
import matt.kbuild.socket.port
import matt.kbuild.socket.SingleSender
import matt.kbuild.socket.MultiSender
import matt.kbuild.proc
import matt.kbuild.allStdOutAndStdErr
import matt.kbuild.cap
import matt.kbuild.git.ExecGit
import matt.kbuild.git.SimpleGit
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Exec

//import matt.kbuild.isMac
//import matt.kbuild.isNewMac

val tempOutputFile = matt.kbuild.tempOutputFile
val WINDOWS = matt.klib.sys.Machine.WINDOWS
val NEW_MAC = matt.klib.sys.Machine.NEW_MAC
val OLD_MAC = matt.klib.sys.Machine.OLD_MAC
val thisMachine: matt.klib.sys.Machine get() = matt.kbuild.thisMachine
val ismac get() = matt.kjlib.lang.jlang.ismac
val isNewMac get() = matt.kbuild.isNewMac

fun <T> T.recarse(includeSelf: Boolean = true, rchildren: (T)->Iterable<T>): Sequence<T> =
  recurse(includeSelf, rchildren)

fun part(name: String) = port(name)


fun SingleSender.open(file: File) = open(file.absolutePath)


enum class ModType { APP, CLAPP, APPLIB, LIB, ABSTRACT }

//enum class Machine {
//  OLD_MAC,
//  NEW_MAC,
//  WINDOWS
//}
//
//val thisMachine by lazy {
//  if (isMac) {
//	if (isNewMac) Machine.NEW_MAC else Machine.OLD_MAC
//  } else Machine.WINDOWS
//  // TODO: CHECK LINUX
//
//}
//
//val isMac by lazy { "mac" in System.getProperty("os.name").toLowerCase() }


val desktopFile by lazy { File(System.getProperty("user.home")).resolve("Desktop") }


fun makeAU3(superproject: String, subproject: String, subprojectDir: File) {
  val mainClassName = subproject.cap().plus("Main")
  val mainKt = "$mainClassName.Kt"
  desktopFile.resolve("$subproject.au3").writeText(
	"""
      #cs ----------------------------------------------------------------------------

       AutoIt Version: 3.3.16.0
       Author:         myName

       Script Function:
      	Template AutoIt script.

      #ce ----------------------------------------------------------------------------

      ; Script Start - Add your code below here

      ${"$"}program="C:\Users\mgrot\AppData\Local\JetBrains\Toolbox\apps\IDEA-U\ch-0\213.6777.52\bin\idea64.exe"
	  ${"$"}file="${
	  subprojectDir.resolve("src").resolve("main").resolve("kotlin").resolve("matt").resolve(superproject)
		.resolve(subproject).resolve(mainKt).absolutePath
	}"
      ; Run(@ComSpec & "/c start " & ${"$"}program & " " & ${"$"}file)
      Run(${"$"}program & " " & ${"$"}file)
      ; Opt("WinTitleMatchMode", 2) ; 2 = Match any substring in the title
      ; WinActivate("$mainClassName")
      Sleep(500)
      WinActivate("MyDesktop – $mainKt [MyDesktop.KJ.${superproject}.${subproject}.main]")
    """.trimIndent()
  )
}


//val isNewMac by lazy {
//  isMac && shell("uname", "-m").trim() == "arm64"
//}

fun err(s: String): Nothing = matt.kjlib.lang.err(s)

inline fun <T> Iterable<T>.firstOrErr(msg: String, predicate: (T)->Boolean): T {
  for (element in this) if (predicate(element)) return element
  matt.kjlib.lang.err(msg)
}

val Project.gitFolder get() = projectDir.listFiles()!!.first { it.name == ".git" }
val Project.isGitProject get() = ".git" in projectDir.list()!!


private val simpleGits = mutableMapOf<Project, SimpleGit>()
val Project.simpleGit: SimpleGit
  get() {
	return simpleGits[this] ?: SimpleGit(gitDir = this.gitFolder.absolutePath).also { simpleGits[this] = it }
  }


fun Project.execGitFor(task: Exec) = ExecGit(task = task, dir = this.gitFolder.absolutePath)
fun File.execGitFor(task: Exec) = takeIf { this.isDirectory && ".git" in this.list()!! }!!.let { f ->
  ExecGit(
	task = task, dir = f.resolve(".git").absolutePath
  )
}

fun Project.setupMavenTasks(compileKotlinJvmTaskName: String) {

  val sp = this

  val lastVersionFile = sp.projectDir.resolve("lastversion.txt")
  var firstPublish = !lastVersionFile.exists()
  if (firstPublish) lastVersionFile.writeText("0")
  var thisVersion = lastVersionFile.readText().toInt() + 1
  sp.version = thisVersion.toString()

  sp.tasks.apply {
	val ck = this.getAt(compileKotlinJvmTaskName)

	ck.doLast("pleasework", object: Action<Task> {
	  override fun execute(t: Task) {
		if (firstPublish || t.didWork) {
		  lastVersionFile.writeText(thisVersion.toString())
		}
	  }
	})

	this.getAt("publishToMavenLocal").apply {
	  dependsOn(sp.tasks.getAt("jar"))
	  this.setOnlyIf(object: Spec<Task> {
		override fun isSatisfiedBy(element: Task?): Boolean {
		  return firstPublish || ck.didWork
		}
	  })
	}

	//	  kjProj.afterEvaluate {

	sp.afterEvaluate {
	  /*this is only for gradle plugins*/
	  if (sp.tasks.map { it.name }.contains("publishPluginMavenPublicationToMavenLocal")) {
		getAt("publishPluginMavenPublicationToMavenLocal").onlyIf(
		  object: Spec<Task> {
			override fun isSatisfiedBy(element: Task?): Boolean {
			  return firstPublish || ck.didWork
			}
		  }
		)
	  }
	}
  }
}

