package plugin;
import org.gradle.api.*;

class HelloPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('hello') {
            println "Hello from ${project.name}"
        }
    }

    void compileProjectOrArtifact() {

    }
}
