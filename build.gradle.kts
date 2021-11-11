plugins {
    id("java")

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    // kotlin("jvm") version "1.4.30"

    // Apply the application plugin to add support for building a CLI application.
    application
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    // implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(files("lib/gurobi.jar"))
    implementation(files("lib/gurobi-javadoc.jar"))

    // Use the Kotlin JDK 8 standard library.
    // implementation(kotlin("stdlib-jdk8"))

    implementation("org.dom4j:dom4j:2.1.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
}

application {
    applicationName = "itc2021"
    group = "br.ufop.itc2021"

    // Define the main class for the application.
    mainClass.set("br.ufop.itc2021.Main")

    version = "0.1.0"
}

tasks {
    // compileKotlin {
    //     sourceCompatibility = JavaVersion.VERSION_15.toString()
    //     targetCompatibility = JavaVersion.VERSION_15.toString()
    //     kotlinOptions {
    //         includeRuntime = true
    //         jvmTarget = "15"
    //     }
    // }

    jar {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes["Main-Class"] = application.mainClass
        }
        archiveFileName.set("${application.applicationName}.jar")
        destinationDirectory.set(rootDir)
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
    }
}
