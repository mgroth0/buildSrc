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
//
//@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
//inline fun <reified T: Task> TaskContainer.withType(cls: Class<T>, cfg: Action<T>) {
//  this.withType(cls).all(cfg)
//}