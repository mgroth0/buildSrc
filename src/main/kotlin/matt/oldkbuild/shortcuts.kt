@file:Suppress("PackageDirectoryMismatch")


import java.io.File
import matt.kbuild.recurse
import matt.kbuild.port
import matt.kbuild.Sender
import matt.kbuild.proc
import matt.kbuild.allStdOutAndStdErr
import org.gradle.configurationcache.extensions.capitalized

//import matt.kbuild.isMac
//import matt.kbuild.isNewMac

val tempOutputFile = matt.kbuild.tempOutputFile
val WINDOWS = matt.kbuild.Machine.WINDOWS
val NEW_MAC = matt.kbuild.Machine.NEW_MAC
val OLD_MAC = matt.kbuild.Machine.OLD_MAC
val thisMachine: matt.kbuild.Machine get() = matt.kbuild.thisMachine
val ismac get() = matt.kbuild.ismac
val isNewMac get() = matt.kbuild.isNewMac

fun <T> T.recarse(includeSelf: Boolean = true, rchildren: (T)->Iterable<T>): Sequence<T> =
  recurse(includeSelf, rchildren)

fun part(name: String) = port(name)


fun Sender.open(file: File) = open(file.absolutePath)


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
  val mainClassName = subproject.capitalized().plus("Main")
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


fun shell(vararg args: String, debug: Boolean = false, workingDir: File? = null): String {
  if (debug) {
	println("running command: ${args.joinToString(" ")}")
  }
  val p = proc(
	wd = workingDir,
	args = args
  )
  val output = p.allStdOutAndStdErr()
  if (debug) {
	println("output: ${output}")
  }
  return output
}

//val isNewMac by lazy {
//  isMac && shell("uname", "-m").trim() == "arm64"
//}

fun err(s: String): Nothing = matt.kbuild.err(s)

inline fun <T> Iterable<T>.firstOrErr(msg:  String,predicate: (T) -> Boolean): T {
  for (element in this) if (predicate(element)) return element
  matt.kbuild.err(msg)
}
