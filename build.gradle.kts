plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "me.mypvp"
description = "NMS Assist for easily working with nms classes"

if (project.hasProperty("projVersion") && project.property("projVersion").toString().isNotEmpty()) {
    project.version = project.property("projVersion").toString()
} else {
    project.version = "0.2.0-SNAPSHOT"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")
    implementation("org.objenesis:objenesis:3.3")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://repo.mypvp.me/repository/maven/")
            credentials {
                username = if (project.hasProperty("mypvpRepoUser"))
                    project.property("mypvpRepoUser").toString()
                    else System.getenv("MYPVP_REPO_USER")
                password = if (project.hasProperty("mypvpRepoPass"))
                    project.property("mypvpRepoPass").toString()
                    else System.getenv("MYPVP_REPO_PASS")
            }
        }
    }
}

tasks {
    java {
        withSourcesJar()
        withJavadocJar()
    }

    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}