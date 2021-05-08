package matt.jbuild.greeting;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import matt.jbuild.greeting.JGreetingPluginExtension;

public class JGreetingPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        JGreetingPluginExtension extension = project.getExtensions()
                .create("jgreeting", JGreetingPluginExtension.class);
        project.task("jhello")
                .doLast(task -> System.out.println("Hello JGradle!"));
    }
}