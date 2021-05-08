package matt.groovyland

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property

//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

abstract class HelpersPluginExtension {
    abstract Property<Boolean> exampleProp()

    HelpersPluginExtension() {
        exampleProp.convention(false)
    }
}

class HelpersPlugin implements Plugin<Project> {
    void apply(Project project) {

        def extension = project.extensions.create(
                'helpers', HelpersPluginExtension
        )

        project.metaClass.helperFun = { ->
            println("I help")
        }

    }
}

