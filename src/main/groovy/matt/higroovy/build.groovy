package matt.higroovy

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication

static def helloGroovy() {
    System.out.println("hi groovy!!!")
}

abstract class ArtifactPluginExtension {
    abstract Property<Boolean> getUsesMinVersion()

    ArtifactPluginExtension() {
        usesMinVersion.convention(false)
    }
}



class ArtifactPlugin implements Plugin<Project> {
    void apply(Project project) {
        def extension = project.extensions.create('greeting', ArtifactPluginExtension)
        project.task('hello') {
            doLast {
                println "hello" + extension.usesMinVersion.get().toString()
            }
        }


//        project.plugins {
//            id 'maven-publish'
//            id 'java'
//        }







//        project.apply plugin: 'maven-publish'
        project.apply plugin: 'java'

        project.java {
            withSourcesJar()
        }

        Project.metaClass.didCompileOrJarWork = { ->
            tasks["jar"].didWork || tasks["compileKotlin"].didWork
        }


        RegularFile buildNumFile = project.layout.projectDirectory.file("lastBuildNum.txt")
        if (!buildNumFile.asFile.exists()) {
            buildNumFile.asFile.text = "0"
        }
        RegularFile lastBuildVersion = project.layout.projectDirectory.file("lastBuildVersion.txt")
        String thisBuild = (buildNumFile.asFile.text.trim().toInteger() + 1).toString()
        project.group = "matt"
// TODO: Enforce incrementing patch numbers when src changes
        project.version = "0.0.1-1"
        String thisVersion = "${project.version}-${thisBuild}".trim()
        String snapVersion = "${project.version}-SNAPSHOT".trim()

        project.publishing {
            publications {
                build(MavenPublication) {
                    artifactId project.name
                    System.out.println("cfg version of ${project.name} to ${thisVersion} 1")
                    version thisVersion
                    System.out.println("cfg version of ${project.name} to ${thisVersion} 2")
                    from project.components.java
//                    versionMapping {
//                        usage("java-api") {
//                            fromResolutionOf("runtimeClasspath")
//                        }
//                    }
                }
                snap(MavenPublication) {
                    artifactId project.name
                    version snapVersion
                    from project.components.java
//                    versionMapping {
//                        usage("java-api") {
//                            fromResolutionOf("runtimeClasspath")
//                        }
//                    }
                }
            }
        }
        Task pub = project.tasks["publishToMavenLocal"]
        project.publishToMavenLocal {
            onlyIf { project.didCompileOrJarWork() }
            doFirst {
                System.out.println("updating buildNumFile of ${project.name} to " + thisBuild)
                buildNumFile.asFile.text = (thisBuild)
                System.out.println("updating lastBuildVersion of ${project.name} to " + thisVersion)
                lastBuildVersion.asFile.text = (thisVersion)
            }
            doLast {
                println("ACTUALLY PUBLISHED ${project.name} ${thisVersion}")
            }
        }
//        project.generateMetadataFileForMavenPublication {
//            onlyIf { project.didCompileOrJarWork() }
//        }
        project.publishBuildPublicationToMavenLocal {
            onlyIf { project.didCompileOrJarWork() }
        }
        project.publishSnapPublicationToMavenLocal {
            onlyIf { project.didCompileOrJarWork() }
        }
// NOTE: aparently need to run this every time or i get an error
//generatePomFileForMavenPublication {
//    onlyIf { tasks["jar"].didWork || tasks["compileKotlin"].didWork }
//}


        // NOTE: can I avoid registering task completely if minversion not used?
        project.tasks.register("publishMin") {
            dependsOn pub
            onlyIf { extension.usesMinVersion.get() && pub.didWork }
            doLast {
                RegularFile minBuildNumFile = project.layout.projectDirectory.file("minVersion.txt")
                System.out.println("updating minBuildNumFile of ${project.name} to " + thisVersion)
                minBuildNumFile.asFile.text = (thisVersion)
            }
        }


    }
}

