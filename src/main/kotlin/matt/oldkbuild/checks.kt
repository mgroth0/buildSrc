@file:Suppress("PackageDirectoryMismatch")

//import matt.kbuild.FLOW_FOLDER
import matt.klib.commons.FLOW_FOLDER
import matt.klib.log.warn
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant
import java.time.Year
import java.util.Calendar
import java.util.Date
import matt.kjlib.git.gitSubmodules
import matt.kjlib.git.ignore.GitIgnore
import matt.klib.commons.get
import matt.klib.str.lower
import matt.klib.str.upper
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.intellij.lang.annotations.Language
import javax.inject.Inject

//import matt.klib.listsEqual

/*all task classes have to be open I think*/

open class MValidations @Inject constructor(rootProjectFolder: File): DefaultTask() {

  @Input
  val abstracInput = System.currentTimeMillis()

  /*THIS IS NECCESARY FOR UP-TO-DATE CHECKS!!!!!*/
  @OutputFile val outputFile = project.buildDir.resolve("reports").resolve("MValidations.txt")


  @TaskAction fun validate() {


	withTimer("validate", quiet = true) {
	  val r = this.project.validate()
	  outputFile.parentFile.mkdirs()
	  if (!outputFile.exists() || outputFile.readText() != r) {
		outputFile.writeText(r)
	  }
	}


  }


}

val EXPLANATIONS_FOLD = FixedFile(FLOW_FOLDER!!.resolve("explanations"))

val normalLanguages = listOf("kotlin", "java", "resources")
val normalSourceSets = listOf("main", "test", "commonMain", "jvmMain")
val testSourceSets = listOf(normalSourceSets[1])

//@OptIn(ExperimentalStdlibApi::class)
private fun Project.validate(): String {

  this.simpleGit.gitSubmodules.filter { it.name != "buildSrc" }.filter { it.name != "RootFiles" }.forEach {
	val expected = ":" + it.path.replace(File.separator, ":").toUpperCase()
	ensure(expected in this.allprojects.map { it.path.toUpperCase() }) {
	  println("expected=$expected")
	  allprojects.forEach {
		println("\t${it.path}")
	  }
	  "${it.name} should be a gradle subproject. All git submodules should be gradle projects so I can properly automate their git-related tasks"
	}
  }


  allprojects {    /*it.*/dir.resolve("src").listFiles()?.forEach {
	ensure(it.name in normalSourceSets || it.name == ".DS_Store") {
	  "\"${it.name}\"? No. " + (EXPLANATIONS_FOLD["noWeirdSrcSets.txt"].takeIf { it.exists() }?.readText() ?: "")
	}
  }


	/*I had trouble a year or so ago, but now I'm much smarter. lets give this a chance (reimplemented maven publish in may of 2022)*/
	if (Calendar.getInstance().get(Calendar.YEAR) > 2022) {
	  this.pluginManager.findPlugin("maven-publish")?.let {

		println("PROJ: dollarSign it")
		bad(
		  "\n\nNo.\n\n" + (EXPLANATIONS_FOLD["noMavenPublish.txt"].takeIf { it.exists() }?.readText()?.trimIndent()
			?: "")
		)
	  }
	}


  }
  (subprojects.map { it.projectDir } + simpleGit.gitSubmodules.map { rootDir[it.path] }).forEach { projFold ->
	val gitIgnore = projFold[".gitignore"]
	val hasBuildFolder = "build" in projFold.list()!!

	if (hasBuildFolder) {
	  ensure(gitIgnore.exists()) {
		"I think ${this} needs a .gitignore file since it has a build folder"
	  }
	  val patterns = GitIgnore(gitIgnore.readText()).patterns
	  val expectedPatterns = mutableListOf("/build/")
	  expectedPatterns += ".gradle/"
	  expectedPatterns += "/gradle/"
	  expectedPatterns += "/gradlew"
	  expectedPatterns += "/gradlew.bat"
	  expectedPatterns += "/lastversion.txt"
	  expectedPatterns += ".DS_Store"
	  expectedPatterns += ".idea/"
	  expectedPatterns += ".vagrant/"
	  expectedPatterns += "/temp/"
	  expectedPatterns += "/tmp/"
	  expectedPatterns += "/data/"
	  expectedPatterns += "/cfg/"
	  expectedPatterns += "/cache/"
	  expectedPatterns += "/jar/"
	  expectedPatterns += "/jars/"
	  expectedPatterns += "/log/"
	  expectedPatterns += "/logs/"
	  expectedPatterns += "/bin/jar/"
	  if (projFold.name.upper() in listOf("KJ", "K").map { it.upper() } || projFold == rootDir) {
		/*RootFiles*/
		expectedPatterns += "/build.gradle.kts"
		expectedPatterns += "/settings.gradle.kts"
		expectedPatterns += "/gradle.properties"
		expectedPatterns += "/shadow.gradle"
	  }
	  if (projFold.name.upper() == "FLOW".upper()) {
		expectedPatterns += "/explanations/"
		expectedPatterns += "/unused_cool/"
		expectedPatterns += "/icon/"
		expectedPatterns += "/status/"
	  }

	  ensure(matt.klib.lang.listsEqual(expectedPatterns, patterns)) {
		println("automatically write correct gitignore for ${projFold}?")
		val answer = readLine()
		var wasFixed = ""
		if (answer!!.lower() in listOf("yes", "y")) {
		  gitIgnore.writeText(expectedPatterns.joinToString("\n"))
		  wasFixed = " (was fixed)"
		}
		"""non-standard .gitignore for ${projFold}${wasFixed}"""
	  }


	}
  }

  var buildSrcBS = Rdir.resolve("buildSrc").resolve("build.gradle.kts")
  if (!buildSrcBS.exists()) {
	buildSrcBS = Rdir.resolve("buildSrc").resolve("build.gradle")
  }
  if (buildSrcBS.exists()) {

	val pluginsBlock = buildSrcBS.readText().substringAfter("plugins").substringBefore("}")

	val usingKotlinDSLInBuildSrc = "kotlin-dsl" in buildSrcBS.readText()
	val usingKotlinInBuildSrc = "kotlin" in pluginsBlock
	val usingEither = usingKotlinDSLInBuildSrc || usingKotlinInBuildSrc


	ensure(usingEither) {

	  "No! " + (EXPLANATIONS_FOLD["noWrongKtVersion.txt"].takeIf { it.exists() }?.readText()?.trimIndent() ?: "")
	}


  }

  val packs = mutableListOf<PackageInfo>()
  val ppis = mutableListOf<ProjectPackInfo>()
  allprojects.forEach {
	val ppi = ProjectPackInfo(it)
	ppis.add(ppi)
  } //  allprojects { proj: Project ->
  //
  //  }
  ppis.forEach {
	it.packs?.forEach { p1 ->
	  packs.add(p1)
	}
  }


  val rootPPi = ppis.firstOrErr("err1") { it.project == rootProject }

  fun attatchFam(ppi: ProjectPackInfo) {
	ppi.dir.listFiles()!!.forEach fi@{ fi ->
	  ppis.asSequence().filter {
		it != ppi && it != rootPPi
	  }.filter {
		it.parent == null
	  }.forEach {
		if (it.dir == fi) {
		  it.parent = ppi
		  ppi.children.add(it)
		  return@fi
		}
	  }
	}
	ppi.children.forEach { attatchFam(it) }
  }
  attatchFam(rootPPi)
  val nonRootPPis = ppis.filter { it != rootPPi }
  ensure(nonRootPPis.all { it.parent != null }) {
	"""
	  ${nonRootPPis.first { it.parent == null }} has no parent 
	""".trimIndent()
  }

  ppis.forEach {
	it.mname = when {
	  it == rootPPi                 -> ""
	  it.parent == rootPPi          -> "matt." + it.name
	  it.parent!!.parent == rootPPi -> "matt." + it.name
	  else                          -> {
		require(rootPPi.dir["KJ"].absolutePath in it.dir.absolutePath) {
		  """not ready to deal with non-KJs yet"""
		}
		"matt." + it.dir.relativeTo(rootPPi.dir["KJ"]).path.removePrefix(File.separator).removeSuffix(File.separator)
		  .replace(File.separator, ".")
	  }
	}
  }

  packs.forEach { pack ->
	if (pack.hasSourceFiles) {
	  ensure(pack.name.startsWith(pack.ppi.mname!!)) {
		"""
		  package ${pack.name} should start with ${pack.ppi.mname}
		  see: ${pack.f.absolutePath}
		  
		  
		""".trimIndent()

	  }
	}


	if (!pack.ppi.isJS) {    /*this check was automatic wth j9Jigsaw, but i'm not doing that any more so gotta do it myself*/

	  pack.sourceFiles.forEach file@{ f ->        //	  TODO: File search text and stop reading if found
		val searchingFor = "package ${pack.name}"
		val reader = f.reader().buffered()
		for (line in reader.lines()) {
		  if (searchingFor in line && line.replace(searchingFor, "").replace(";", "").isBlank()) {
			reader.close()
			return@file
		  } else if ("package" in line) {
			reader.close()
			bad(
			  """
			
			package declaration in ${f.absolutePath} is $line but should be $searchingFor
			
		  """.trimIndent()
			)
		  }
		}
		bad(
		  """
			
			no package declaration in ${f.absolutePath}
			
		  """.trimIndent()
		)

	  }
	}
  }





  ppis.filter { it.srcExists }.forEach { ppi ->
	ensure(ppi.srcSets!!.isNotEmpty()) { "why is src of $ppi empty?" }

	ppi.srcSets.forEach {
	  ensure(it.name in normalSourceSets) { "VERY BAD since this was already checked above..." }
	}
  }



  packs.forEach { p1 ->
	ensure(p1.names.count() > 1 || p1.sourceFiles.count() == 0) {
	  """
		do I really want to have just a "matt" package?
		see: ${p1.f.absolutePath}
		
		""".trimIndent()
	}
	ensure(p1.names[0] == "matt") {


	  """
		
		PACKAGE SHOULD START WITH "matt", not "${p1.names[0]}":
		${p1.f.absolutePath}
		
		good to start all packages with \"matt\". 1. organization, 2. I think I already have some reflection that depends on this 3. In the future I possibly/likely will have even more reflection depending on this""".trimIndent()
	}
	if (p1.subpackages.isEmpty()) {
	  ensure(p1.sourceFiles.count() <= 1) {

		"""
		${p1.name} has multiple source files
		see: ${p1.f.absolutePath}
		
		lets have one source file per package so that imports at the top of kotlin (+java?+groovy?+scala?, but def kt) files are a clear indication of who this code uses. Feel free to move to a different package in the same module
		
	  """.trimIndent()

	  }
	  if (p1.sslpi.name != "resources") {
		ensure(p1.sourceFiles.isNotEmpty()) {

		  """
		${p1.name} has no source files
		see: ${p1.f.absolutePath}
		
 		this is just disorganized
		
	  """.trimIndent()

		}
		if (p1.isMainPack) {
		  ensure(
			p1.sourceFiles[0].nameWithoutExtension.equals(
			  p1.names.last() + "Main", ignoreCase = true
			) || p1.sourceFiles[0].nameWithoutExtension.equals(p1.names.last(), ignoreCase = true)
		  ) /*being a bit lazy. non-executable modules like libraries should NOT have "Main" in any src names. Used an or statement here as a shortcut bc I don't have time to check if the current module is a library now. I'm not sure if I've set this up yet*/{

			"""
		${p1.name}'s source file is not the same (+Main since main package) (even ignoring case) as the package name
		see: ${p1.f.absolutePath}
		
 		this is just disorganized, by my standards, since I'm going for no more than 1 src file per package
	
		p1.sourceFiles[0].nameWithoutExtension=${p1.sourceFiles[0].nameWithoutExtension}
		p1.names.last()=${p1.names.last()} 
	  """.trimIndent()
		  }
		} else {
		  ensure(p1.sourceFiles[0].nameWithoutExtension.equals(p1.names.last(), ignoreCase = true)) {

			"""
		${p1.name}'s source file is not the same (even ignoring case) as the package name
		see: ${p1.f.absolutePath}
		
 		this is just disorganized, by my standards, since I'm going for no more than 1 src file per package
		
	  """.trimIndent()

		  }
		}
	  }
	} else {    /*  ensure(p1.sourceFiles.isEmpty()) {

		  """
		  ${p1.name} has both subpackages and a source file
		  see: ${p1.f.absolutePath}

		   this is disorganized?

		""".trimIndent()

		}*/
	}
	packs.forEach { p2 ->
	  if (p1.hasSourceFiles && p2.hasSourceFiles && p1 != p2) {
		if (p1.name == p2.name) {
		  ensure(p1.ppi.isMultiplatform || (p1.isTest || p2.isTest)) {
			"""
			  if there is a duplicate package name, one should be a test
			  
			  see:
			  	${p1.f.absolutePath}
			  	${p2.f.absolutePath}
			  
			  
			  """.trimIndent()
		  }
		  ensure(!(p1.isTest && p2.isTest)) {
			"they should not both be tests"
		  }
		  ensure(p1.ppi == p2.ppi) {
			"$p1 and $$p2 should be in the same project, but $p1 is in ${p1.ppi} and p2 is in ${p2.ppi}"
		  }
		}
	  }
	}
  }
  return "success"
}

class CheckFailedException(message: String): RuntimeException(message)

private fun bad(m: String): Nothing = throw CheckFailedException(m)


class ProjectPackInfo(val project: Project) {
  override fun toString(): String {
	return "${ProjectPackInfo::class.simpleName} for $project"
  }

  val name = project.name
  val isJS = name == "kjs"
  val dir = project.dir
  val src = dir["src"]
  val isMultiplatform = dir.hasParentWithNameStartingWith("k")
  val srcExists = src.exists()
  val srcSets = src.listFiles()?.filter { it.name != ".DS_Store" }?.map { SourceSetPackInfo(it, this) }
  val langs = srcSets?.flatMap { it.langFolds }?.apply {
	val lfolds = map { it.name }.toSet()
	if (false) {
	  require(lfolds.count() == 1) {
		"sure you want multiple languages in one module?: ${lfolds.joinToString()}"
	  }
	}
  }

  val packs = langs?.flatMap { it.packages }

  var parent: ProjectPackInfo? = null
  val children = mutableListOf<ProjectPackInfo>()
  var mname: String? = null
}

/*example SourceSetPackInfo: /Users/matt/Desktop/registered/todo/flow/kjs/src/main*/
class SourceSetPackInfo(val srcSet: FixedFile, val ppi: ProjectPackInfo) {

  override fun toString(): String {
	return "${SourceSetPackInfo::class} with srcSet=$srcSet,ppi=$ppi"
  }

  companion object {
	var firstMade = false
  }

  init {
	if (!firstMade) {    //	  println("example SourceSetPackInfo: ${srcSet.absolutePath}")
	  firstMade = true
	}
	require(srcSet.isDirectory) {
	  "$srcSet should be a dir"
	}
  }

  val name = srcSet.name
  val isTest = name in testSourceSets

  val langFolds = srcSet.listFiles()!!.onEach {
	if (it.name == "module-info.java") return@onEach
	require(it.isDirectory) {
	  "sub srcSet folder should be a folder not a file like $it"
	}
	require(it.name in normalLanguages) {
	  "weird language: $it"
	}
  }.map {
	SourceSetLanguagePackInfo(it, this)
  }


}


/*example SourceSetLanguagePackInfo: /Users/matt/Desktop/registered/todo/flow/kjs/src/main/kotlin*/
class SourceSetLanguagePackInfo(val f: FixedFile, val sspi: SourceSetPackInfo) {

  override fun toString(): String {
	return "${SourceSetLanguagePackInfo::class} with f=$f,sspi=$sspi"
  }

  val packages: List<PackageInfo>
  val name = (f.name)

  val ppi = sspi.ppi

  companion object {
	var firstMade = false
  }

  init {
	if (!firstMade) {    //	  println("example SourceSetLanguagePackInfo: ${f.absolutePath}")
	  firstMade = true
	}
	val packs = mutableListOf<PackageInfo>()
	f.recarse(
	  includeSelf = false
	) {
	  it.listFiles()?.map { it }?.toList() ?: listOf()
	}.forEach {
	  if (it.isDirectory) {
		packs.add(PackageInfo(it, this))
	  }

	}
	packages = packs
  }
}

class PackageInfo(val f: FixedFile, val sslpi: SourceSetLanguagePackInfo) {
  val sspi = sslpi.sspi
  val ppi = sspi.ppi
  val name =
	f.absolutePath.replace(sslpi.f.absolutePath, "").replace(File.separator, ".").removePrefix(".").removeSuffix(".")

  override fun toString(): String {
	return "${PackageInfo::class} with f=$f,sslpi=$sslpi"
  }

  val names = name.split(".")
  val isTest = sspi.isTest
  val isMainPack = name.equals(
	"matt.${
	  f.absolutePath.substringAfter(
		sslpi.f        /*sslpi.sspi.ppi.project.rootProject.projectDir*/

		  /*.resolve("KJ")
		  .resolve(sslpi.sspi.ppi.project.name)*/        /*.resolve("src")
		  .resolve("main")
		  .resolve(sslpi.name)*/.resolve("matt") /*redundant*/.absolutePath

	  ).trimStart(File.separatorChar).trimEnd(File.separatorChar).replace(File.separator, ".")
	}", ignoreCase = true
  )/*.apply {
	println("isMainPack for ${this@PackageInfo.name}=${this}")
	println("\t${f.absolutePath} substring after...")
	println(
	  sslpi.sspi.ppi.project.rootProject.projectDir
		.resolve("KJ")
		.resolve(sslpi.sspi.ppi.project.name)
		.resolve("src")
		.resolve("main")
		.resolve(sslpi.name)
		.resolve("matt") *//*redundant*//*
		.absolutePath
	)
	println(
	  "\texpected ${
		"matt.${
		  f.absolutePath.substringAfter(
			sslpi.sspi.ppi.project.rootProject.projectDir
			  .resolve("KJ")
			  .resolve(sslpi.sspi.ppi.project.name)
			  .resolve("src")
			  .resolve("main")
			  .resolve(sslpi.name)
			  .resolve("matt") *//*redundant*//*
			  .absolutePath
		  )
			.trimStart(File.separatorChar)
			.trimEnd(File.separatorChar)
			.replace(File.separator, ".")
		}"
	  }"
	)
  }*/

  /*redundant*/
  val subpackages = f.listFiles()!!.filter { it.isDirectory }.map {
	PackageInfo(it, sslpi)
  }

  val javaFiles = f.listFiles()!!.filter { !it.isDirectory && it.extension == "java" }
  val kotlinFiles = f.listFiles()!!.filter { !it.isDirectory && it.extension == "kt" }
  val sourceFiles = javaFiles + kotlinFiles
  val hasSourceFiles = sourceFiles.count() >= 1

  init {
	if (sslpi.name != "resources") {
	  f.listFiles()!!.forEach {
		require(it.isDirectory || it.extension == "java" || it.extension == "kt" || it.name == ".DS_Store") {
		  "weird source file: ${it.absolutePath}"
		}
	  }
	}
  }
}

fun ensure(test: Boolean, lazyMessage: ()->String) {
  if (!test) bad(lazyMessage())
}