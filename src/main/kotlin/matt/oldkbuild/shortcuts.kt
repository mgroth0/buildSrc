@file:Suppress("PackageDirectoryMismatch")


import java.io.File

import matt.kjlib.stream.recurse.recurse
import matt.kbuild.proc
import matt.kbuild.allStdOutAndStdErr
import matt.klib.lang.cap
import matt.kbuild.git.ExecGit
import matt.kbuild.git.SimpleGit
import matt.kjlib.socket.SingleSender
import matt.kjlib.socket.port
//import matt.kbuild.socket.SingleSender
//import matt.kbuild.socket.port
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Exec

//import matt.kbuild.isMac
//import matt.klib.commons.isNewMac

//val tempOutputFile = matt.kbuild.tempOutputFile
val WINDOWS = matt.klib.sys.Machine.WINDOWS
val NEW_MAC = matt.klib.sys.Machine.NEW_MAC
val OLD_MAC = matt.klib.sys.Machine.OLD_MAC
val thisMachine: matt.klib.sys.Machine get() = matt.klib.commons.thisMachine
val ismac get() = matt.klib.commons.ismac
val isNewMac get() = matt.klib.commons.isNewMac
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

val LAST_VERSION_TXT = "lastversion.txt"

fun Project.setupMavenTasks(compileKotlinJvmTaskName: String, jarTaskName: String) {

  val sp = this

  val lastVersionFile = sp.projectDir.resolve(LAST_VERSION_TXT)
  var firstPublish = !lastVersionFile.exists() || lastVersionFile.readText().isBlank()
  //  if (firstPublish) lastVersionFile.writeText("0")
  //  var thisVersion = lastVersionFile.readText().toInt() + 1
  //  sp.version = thisVersion.toString()

  /*I used to use an incrementing version file. But this quickly becomes an arbitrary number too.
  * Using system time is much safer. absolutely no risk or overwriting a previous build, which becomes increasingly likely
  * when a library is being built by sererate projects connected by git. I'm not saying this is the only way to solve that issue (another way could be some global or online version file between projects) but this seems extreemly simple, fast, easy, straightforward, and has the added bonus of including the precise build time right in the versin in case I ever want that data in the future*/
  sp.version = System.currentTimeMillis()

  sp.tasks.apply {
	val ck = this.getAt(compileKotlinJvmTaskName)

	ck.doLast("pleasework", object: Action<Task> {
	  override fun execute(t: Task) {
		if (firstPublish || t.didWork) {
		  lastVersionFile.writeText(sp.version.toString())
		}
	  }
	})

	this.getAt("publishToMavenLocal").apply {
	  /*tasks.forEach {
		println("${it.path}")
	  }*/
	  dependsOn(sp.tasks.getAt(jarTaskName))
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

