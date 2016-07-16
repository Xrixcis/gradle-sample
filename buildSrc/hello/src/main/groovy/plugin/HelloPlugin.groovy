package plugin

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar

class HelloPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        project.task('hello') << {
            println "Hello from ${project.name}"
        }

        project.extensions.create('dependency', DependencyExtension, project)

        project.repositories {
            mavenLocal()
            // TODO
        }

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

            project.publishing.publications {
                println("configuring publications for project ${project.name} - ${project.components.java}")
                java(MavenPublication) {
                    artifactId = project.archivesBaseName
                    from project.components.java
                    artifact project.tasks.sourcesJar
                    artifact project.tasks.javadocJar
                    artifact project.tasks.signedJar
                }
            }
            project.publishing.repositories {
                // TODO
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
