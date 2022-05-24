//include("kbuild")
rootProject.name = "buildSrc" /*necessary to avoid some warning*/
//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")


/**
 * @see <a href="https://github.com/gradle/gradle/issues/19069">Feature request</a>
 */
fun Settings.enableFeaturePreviewQuietly(name: String, summary: String) {

//  if (name != "VERSION_CATALOGS") {
	enableFeaturePreview(name)
//  }


  val logger: Any = org.gradle.util.internal.IncubationLogger::class.java
	.getDeclaredField("INCUBATING_FEATURE_HANDLER")
	.apply { isAccessible = true }
	.get(null)

  @Suppress("UNCHECKED_CAST")
  val features: MutableSet<String> = org.gradle.internal.featurelifecycle.LoggingIncubatingFeatureHandler::class.java
	.getDeclaredField("features")
	.apply { isAccessible = true }
	.get(logger) as MutableSet<String>

  features.add(summary)
}

enableFeaturePreviewQuietly("TYPESAFE_PROJECT_ACCESSORS", "Type-safe project accessors")
//enableFeaturePreviewQuietly("VERSION_CATALOGS", "Type-safe dependency accessors")