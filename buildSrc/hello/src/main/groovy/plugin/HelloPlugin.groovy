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

        // dependencies configuration

        project.extensions.create('dependency', DependencyExtension, project)

        project.repositories {
            mavenLocal()
            // TODO
        }

        // publishing configuration

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

            project.afterEvaluate {
                project.publishing.publications {
//                    println "CONFIGURING PUBLISHING " + project.name
//                    Thread.dumpStack()
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

        // release plugin configuration

        project.tasks.create('useRepositoryDependencies')
        project.apply plugin: 'net.researchgate.release'
        project.release.scmAdapters = [ SvnAdapter ]
        project.release.buildTasks = [ 'useRepositoryDependencies', 'clean', 'build' ]
        // TODO
        project.afterReleaseBuild.dependsOn 'publishToMavenLocal'
    }
}

class DependencyExtension {

    private Project project

    DependencyExtension(Project project) {
        this.project = project
    }

    def project(String name, String artifact) {

        def tasks = project.gradle.startParameter.taskNames
        def isRelease = tasks.contains(project.path + ':checkSnapshotDependencies') ||
                tasks.contains(project.path + ':useRepositoryDependencies')
        if (project.rootProject == project || isRelease) {
            project.logger.lifecycle("${project.name}: using repo dependency for ${name}")
            return artifact
        } else {
            project.logger.lifecycle("${project.name}: using project dependency for ${name}")
            return project.project(name)
        }
    }
}

class SvnAdapter extends net.researchgate.release.SvnAdapter {

    SvnAdapter(Project project, Map<String, Object> attributes) {
        super(project, attributes)
    }

    @Override
    String exec(Map options, List<String> commands) {
        // override the working directory to current project directory instead of root project dir
        options['directory'] = options['directory'] ?: project.projectDir
        return super.exec(options, commands)
    }
}
