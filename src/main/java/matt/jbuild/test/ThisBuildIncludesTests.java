package matt.jbuild.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.testing.Test;

public class ThisBuildIncludesTests implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        MyTestPluginExtension extension = project.getExtensions().create("test", MyTestPluginExtension.class);


        project.getDependencies().add(
                "testImplementation",
                "org.jetbrains.kotlin:kotlin-test-junit5"
        );
        project.getDependencies().add(
                "testImplementation",
                "org.junit.jupiter:junit-jupiter-api:5.6.0"
        );
        project.getDependencies().add(
                "testRuntimeOnly",
                "org.junit.jupiter:junit-jupiter-engine:5.6.0"
        );

        Task t = project.getTasks().getByName("test");
        Test tt = (Test) t;

        tt.useJUnitPlatform();

    }
}
