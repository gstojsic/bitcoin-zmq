import org.jreleaser.model.Active

plugins {
    id("java")
    id("maven-publish")
    id("org.jreleaser") version "1.4.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.javadoc {
    options {
        (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("zmq") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("A java lib to subscribe to bitcoin node zmq notifications")
                url.set("https://github.com/gstojsic/bitcoin-zmq")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:gstojsic/bitcoin-zmq.git")
                    url.set("https://github.com/gstojsic/bitcoin-zmq")
                }
                developers {
                    developer {
                        id.set("gstojsic")
                        name.set("Goran Stojšić")
                        email.set("goran.stojsic@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            nexus2 {
                create("maven-central") {
                    active.set(Active.ALWAYS)
                    url.set("https://s01.oss.sonatype.org/service/local")
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}