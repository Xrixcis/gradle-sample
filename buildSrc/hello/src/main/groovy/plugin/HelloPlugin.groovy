package plugin;
import org.gradle.api.*
import org.gradle.jvm.tasks.Jar;

class HelloPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('hello') << {
            println "Hello from ${project.name}"
        }

        project.extensions.create('dependency', DependencyExtension, project)

        project.repositories.mavenLocal()

        if (project.plugins.findPlugin('java')) {
            project.task([type: Jar, dependsOn: 'classes'], 'sourcesJar') {
                classifier = 'sources'
                from project.sourceSets.main.allSource
            }

            project.task([type: Jar, dependsOn: 'javadoc'], 'javadocJar') {
                classifier = 'javadoc'
                from project.javadoc.destinationDir
            }

            project.task([type: Jar, dependsOn: 'classes'], 'signedJar') {
                classifier = 'signed'
                from project.sourceSets.main.output
                doLast {
                    project.exec {
                        def signer = new File(System.getProperty('java.home') + '/../bin/jarsigner')
                        if (!signer.exists()) {
                            throw new GradleException("jarsigner not found (${signer.getAbsolutePath()})")
                        }
                        executable signer
                        // TODO real arguments
                        args '-verify'
                        args archivePath
                    }
                }
            }
        }
    }
}

class DependencyExtension {

    private Project project;

    DependencyExtension(Project project) {
        this.project = project
    }

    def project(String name, String artifact) {

        if (project.rootProject == project) {
            project.logger.lifecycle("${project.name}: using repo dependency for ${name}")
            return artifact
        } else {
            project.logger.lifecycle("${project.name}: using project dependency for ${name}")
            return project.project(name)
        }
    }
}
