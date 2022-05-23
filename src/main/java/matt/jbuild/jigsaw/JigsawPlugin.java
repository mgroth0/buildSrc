package matt.jbuild.jigsaw;

//import matt.groovyland.JigsawPluginExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.Directory;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.impldep.org.intellij.lang.annotations.Language;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile;

import java.io.File;

public class JigsawPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {


        JigsawPluginExtension extension = project.getExtensions().create("test", JigsawPluginExtension.class);


        project.afterEvaluate(p -> {
            p.getTasks().withType(JavaCompile.class, jc -> {

                Task ktSrc = project.getTasks().findByName("compileKotlinJvm");
                if (ktSrc == null) {
                    ktSrc = project.getTasks().getByName("compileKotlin");
                }
                ktSrc = ktSrc;

                File d = ((KotlinCompile) ktSrc).getDestinationDirectory().get().getAsFile();
                ((KotlinCompile) ktSrc).getDestinationDirectory().set(d);
//                ((JavaCompile) jc).setDestinationDir(d);
                /*apparently this duplicates something
                https://stackoverflow.com/questions/47657755/building-a-kotlin-java-9-project-with-gradle*/

            });
        });


        @Language("groovy")
        String groovy = """
                project.afterEvaluate {
                    
                    println("warning! untested on groovy 1")
                        
                    //noinspection GroovyAssignabilityCheck
                    project.tasks.withType(JavaCompile) { Task t ->
                        
                        Task ktSrc = project.tasks["compileKotlinJvm"]
                        project.tasks.find { it.name == "compileKotlinJvm" }
                        if (ktSrc == null) ktSrc = project.tasks["compileKotlin"]
                    //                ktSrc = (KotlinCompile) ktSrc
                        
                        (this as JavaCompile).destinationDir = ktSrc.destinationDir
                        /*apparently this duplicates something
                        https://stackoverflow.com/questions/47657755/building-a-kotlin-java-9-project-with-gradle*/
                        
                        
                    }
                }
                                """;

        @Language("kotlin")
        String kt = """
                if (JIGSAW) {
                    tasks.withType<JavaCompile> {
                        /*apparently this duplicates something
                        https://stackoverflow.com/questions/47657755/building-a-kotlin-java-9-project-with-gradle*/

                        this.destinationDir = (tasks[
                        if (spname == "klib") "compileKotlinJvm" else "compileKotlin"
                        ]
                        as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                        ).destinationDir


                    }
                }
                                """;


    }
}
