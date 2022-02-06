@file:Suppress("PackageDirectoryMismatch")

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/*all task classes have to be open I think*/
open class MValidations: DefaultTask() {
  @TaskAction
  fun validate() {
	withTimer("validate") {
	  this.project.validate()
	}
  }
}

val EXPLANATIONS_FOLD = FixedFile(USER_DIR.resolve("explanations"))

val normalLanguages = listOf("kotlin", "java", "resources")
val normalSourceSets = listOf("main", "test", "commonMain", "jvmMain")
val testSourceSets = listOf(normalSourceSets[1])
private fun Project.validate() {

  gitSubmodules
	.filter { it.first != "buildSrc" }
	.forEach {
	  ensure(":" + it.first.replace("_", ":") in this.allprojects.map { it.path }) {
		"${it.first} should be a gradle subproject. All git submodules should be gradle projects so I can properly automate their git-related tasks"
	  }
	}


  allprojects {
	/*it.*/dir.resolve("src").listFiles()?.forEach {
	ensure(it.name in normalSourceSets || it.name == ".DS_Store") {
	  "\"${it.name}\"? No. " + (EXPLANATIONS_FOLD["noWeirdSrcSets.txt"].takeIf { it.exists() }?.readText() ?: "")
	}
  }


	this.pluginManager.findPlugin("maven-publish")?.let {

	  println("PROJ:$it")
	  bad(
		"\n\nNo.\n\n" + (EXPLANATIONS_FOLD["noMavenPublish.txt"].takeIf { it.exists() }?.readText()?.trimIndent()
		  ?: "")
	  )
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
  allprojects {
	val ppi = ProjectPackInfo(/*it*/this)
	ppis.add(ppi)
  }
  ppis.forEach {
	it.packs?.forEach { p1 ->
	  packs.add(p1)
	}
  }


  val rootPPi = ppis.first { it.project == rootProject }

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
  ensure(
	ppis.filter { it != rootPPi }.all { it.parent != null }
  ) {
	"""
	  ${ppis.filter { it != rootPPi }.first { it.parent != null }.dir} has no parent 
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
		"matt." + it.dir.relativeTo(rootPPi.dir["KJ"]).path.removePrefix("/").removeSuffix("/").replace("/", ".")
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


	if (!pack.ppi.isJS) {
	  /*this check was automatic wth j9Jigsaw, but i'm not doing that any more so gotta do it myself*/

	  pack.sourceFiles.forEach file@{ f ->
		//	  TODO: File search text and stop reading if found
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
			  p1.names.last() + "Main",
			  ignoreCase = true
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
	} else {
	  /*  ensure(p1.sourceFiles.isEmpty()) {

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
		  ensure(p1.isTest || p2.isTest) {
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
			"they should be in the same project"
		  }
		}
	  }
	}
  }
}

class CheckFailedException(message: String): RuntimeException(message)

private fun bad(m: String): Nothing = throw CheckFailedException(m)


class ProjectPackInfo(val project: Project) {
  val name = project.name
  val isJS = name == "kjs"
  val dir = project.dir
  val src = dir["src"]
  val srcExists = src.exists()
  val srcSets = src.listFiles()
	?.filter { it.name != ".DS_Store" }
	?.map { SourceSetPackInfo(it, this) }
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
  companion object {
	var firstMade = false
  }

  init {
	if (!firstMade) {
	  println("example SourceSetPackInfo: ${srcSet.absolutePath}")
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
  val packages: List<PackageInfo>
  val name = (f.name)

  val ppi = sspi.ppi

  companion object {
	var firstMade = false
  }

  init {
	if (!firstMade) {
	  println("example SourceSetLanguagePackInfo: ${f.absolutePath}")
	  firstMade = true
	}
	val packs = mutableListOf<PackageInfo>()
	f.recurse(
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
  val name = f.absolutePath.replace(sslpi.f.absolutePath, "").replace("/", ".").removePrefix(".").removeSuffix(".")
  val names = name.split(".")
  val isTest = sspi.isTest
  val isMainPack = name.equals(
	"matt.${
	  f.absolutePath.substringAfter(
		sslpi.f
		/*sslpi.sspi.ppi.project.rootProject.projectDir*/

		  /*.resolve("KJ")
		  .resolve(sslpi.sspi.ppi.project.name)*/
		  /*.resolve("src")
		  .resolve("main")
		  .resolve(sslpi.name)*/
		  .resolve("matt") /*redundant*/
		  .absolutePath
 
	  )
		.trimStart('/')
		.trimEnd('/')
		.replace("/", ".")
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
			.trimStart('/')
			.trimEnd('/')
			.replace("/", ".")
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