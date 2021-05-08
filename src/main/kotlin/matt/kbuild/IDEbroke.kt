@file:Suppress("PackageDirectoryMismatch")
/*
avoid package statement
so buildscripts dont need to use import statements????
see https://github.com/gradle/gradle/issues/7557
*/
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import java.io.File

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
inline fun <reified T: Task> TaskContainer.withType(cls: Class<T>, cfg: Action<T>) {
  this.withType(cls).all(cfg)
}


class FixedFile(path: String): File(path) {
  constructor(f: File): this(f.absolutePath)

  operator fun get(s: String) = resolve(s)
  operator fun get(f: File) = resolve(f)

  fun resolve(s: String): FixedFile {
	return FixedFile((this as File).resolve(s))
  }

  fun resolve(f: File): FixedFile {
	return FixedFile((this as File).resolve(f))
  }


  override fun listFiles(): Array<FixedFile>? {
	return super.listFiles()?.map { FixedFile(it) }?.toTypedArray()
  }
}

val Project.dir get() = FixedFile(projectDir)
val Project.Rdir get() = FixedFile(rootDir)