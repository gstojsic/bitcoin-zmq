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
    implementation("org.zeromq:jeromq:0.5.3")
    implementation("org.slf4j:slf4j-api:2.0.6")

    testImplementation("io.github.gstojsic.bitcoin:proxy:2.0")
    testImplementation("org.testcontainers:junit-jupiter:1.17.6")
    testImplementation("org.testcontainers:testcontainers:1.17.6")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.6")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
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
                description.set("A java lib for processing bitcoin node zmq notifications")
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
    environment {
        setVariables("jreleaser.toml")
    }

    gitRootSearch.set(true)
    dryrun.set(true)    //disable to release for real

    release {
        github {
            username.set("gstojsic")
            repoOwner.set("gstojsic")
            commitAuthor {
                name.set("goran")
            }
        }
    }

    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        //active.set(Active.NEVER) //disables deploy
        maven {
            nexus2 {
                create("maven-central") {
                    active.set(Active.ALWAYS)
                    url.set("https://s01.oss.sonatype.org/service/local")
                    snapshotUrl.set("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}