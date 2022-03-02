@file:Suppress("PackageDirectoryMismatch")


import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.Reader
import java.net.ConnectException
import java.net.Socket
import java.util.concurrent.Semaphore


fun <T> T.recurse(includeSelf: Boolean = true, rchildren: (T)->Iterable<T>): Sequence<T> {
  val mychildren = rchildren(this).iterator()
  var gaveSelf = false
  var currentChild: Iterator<T>? = null
  return object: Iterator<T> {
	override fun hasNext(): Boolean {
	  if (currentChild != null && currentChild!!.hasNext()) {
		return true
	  }
	  return mychildren.hasNext() || (!gaveSelf && includeSelf)
	}

	override fun next(): T {
	  return if (currentChild != null && currentChild!!.hasNext()) {
		currentChild!!.next()
	  } else if (mychildren.hasNext()) {
		currentChild = mychildren.next().recurse(rchildren = rchildren).iterator()
		next()
	  } else if (!gaveSelf && includeSelf) {
		gaveSelf = true
		this@recurse
	  } else {
		throw RuntimeException("guess I messed up the recursion logic")
	  }
	}
  }.asSequence()
}


fun port(name: String): Int {
  return when (name) {
	"file" -> 65008
	else   -> err("todo")
  }
}

val MY_INTER_APP_SEM = Semaphore(1)

class Sender(
  val key: String
) {
  val sem = MY_INTER_APP_SEM
  fun send(message: String, use_sem: Boolean = true): String? { // return channel
	println("starting send")
	val response: String?
	try {


	  val kkSocket = Socket("localhost", port(key))
	  val out = PrintWriter(kkSocket.getOutputStream(), true)
	  val inReader = BufferedReader(
		InputStreamReader(kkSocket.getInputStream())
	  )
	  println("acquire sem")
	  if (use_sem) sem.acquire()
	  out.println(message)
	  println("getting response...")
	  response = inReader.readWithTimeout(2000)
	  println("got response")
	  kkSocket.close()
	} catch (e: ConnectException) {
	  if (use_sem) sem.release()
	  println(e.message)
	  return null
	}
	if (use_sem) sem.release()
	return if (response == "") {
	  println("recieved no responsed")
	  null
	} else {
	  println(
		"recieved response:${response}"
	  )
	  response
	}
  }

  fun send(pair: Pair<String, String>): String? { // return channel
	return send("${pair.first}:${pair.second}")
  }

  fun receive(message: String) = send(message, use_sem = false)

  @Suppress("unused")
  fun activate() = send("ACTIVATE")


  fun are_you_running(name: String): String? {
	return receive("ARE_YOU_RUNNING:${name}")
  }

  @Suppress("unused")
  fun exit() = send("EXIT")
  fun go(value: String) = send("GO" to value)
  fun open(value: String) = send("OPEN" to value)
}

object InterAppInterface {
  private val senders = mutableMapOf<String, Sender>()
  operator fun get(value: String): Sender {
	return if (senders.keys.contains(value)) {
	  senders[value]!!
	} else {
	  senders[value] = Sender(value)
	  senders[value]!!
	}

  }
}

fun Sender.open(file: File) = open(file.absolutePath)


fun File.openWithPDF() = InterAppInterface["PDF"].open(this)

class NoServerResponseException(servername: String): Exception() {
  override val message = "No response from server: $servername"
}

@Throws(IOException::class)
fun Reader.readWithTimeout(timeoutMillis: Int): String {
  val entTimeMS = System.currentTimeMillis() + timeoutMillis
  var r = ""
  var c: Int
  while (System.currentTimeMillis() < entTimeMS) {
	if (ready()) {
	  c = read()
	  if (c == -1) {
		if (r.isNotEmpty()) return r else throw RuntimeException("bad dup")
	  }
	  r += c.toChar().toString()
	}
  }
  return r
}

enum class ModType { APP, CLAPP, APPLIB, LIB, ABSTRACT }

enum class Machine {
	OLD_MAC,
	NEW_MAC,
	WINDOWS
}

val thisMachine by lazy {
	if (isMac) {
		if (isNewMac) Machine.NEW_MAC else Machine.OLD_MAC
	} else Machine.WINDOWS
	// TODO: CHECK LINUX

}

val isMac by lazy { "mac" in System.getProperty("os.name").toLowerCase() }

fun shell(vararg args: String, debug: Boolean = false, workingDir: File? = null): String {
  if (debug) {
	println("running command: ${args.joinToString(" ")}")
  }
  val proc = ProcessBuilder(*args).apply {
	if (workingDir != null) this.directory(workingDir)
  }.start()
  proc.waitFor()
  val output = BufferedReader(InputStreamReader(proc.inputStream)).readText()
  val errorOutput = BufferedReader(InputStreamReader(proc.errorStream)).readText()
  if (debug) {
	println("output: ${output}")
	println("errorOutput: ${errorOutput}")
  }
  return output
}

val isNewMac by lazy {
  isMac && shell("uname", "-m").trim() == "arm64"
}