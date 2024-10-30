import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    jacoco
    id("org.springframework.boot") version "3.3.+"
    id("org.owasp.dependencycheck") version "11.0.0"
    id("com.github.rising3.semver") version "0.8.2"
    id("io.spring.dependency-management") version "1.1.+"
    // id("org.graalvm.buildtools.native") version "0.10.+"
    id("org.jetbrains.dokka") version "2.0.0-Beta"
	id("com.google.cloud.tools.jib") version "3.4.4"
	kotlin("jvm") version "2.0.+"
	kotlin("plugin.spring") version "2.0.+"
}

group = "io.dereknelson.lostcities"
version = project.property("version")!!

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/lostcities-cloud/lostcities-common")
        credentials {
            username = System.getenv("GH_USER")
            password = System.getenv("GH_TOKEN")
        }
    }
	maven {
		url = uri("https://maven.pkg.github.com/lostcities-cloud/lostcities-models")
		credentials {
            username = System.getenv("GH_USER")
            password = System.getenv("GH_TOKEN")
		}
	}

	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

val ktlint by configurations.creating


dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

configurations.matching { it.name.startsWith("dokka") }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group.startsWith("com.fasterxml.jackson")) {
            useVersion("2.15.3")
        }
    }
}

dependencies {
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    if(  rootProject.hasProperty("debug")){
        implementation(project(":lostcities-common"))
        implementation(project(":lostcities-models"))
    } else {
        implementation("io.dereknelson.lostcities-cloud:lostcities-common:0.0.3")
        implementation("io.dereknelson.lostcities-cloud:lostcities-models:0.0.2")
    }

    implementation("org.springframework.boot:spring-boot-devtools")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")


    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.8.0")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

	ktlint("com.pinterest:ktlint:0.49.1") {
		attributes {
			attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
		}
	}
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.0.0-Beta")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val outputDir = "${layout.buildDirectory}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck by tasks.creating(JavaExec::class) {
	inputs.files(inputFiles)
	outputs.dir(outputDir)

	description = "Check Kotlin code style."
	classpath = ktlint
	mainClass.set("com.pinterest.ktlint.Main")
	args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
	inputs.files(inputFiles)
	outputs.dir(outputDir)
	description = "Fix Kotlin code style deviations."
	classpath = ktlint
	mainClass.set("com.pinterest.ktlint.Main")
	args = listOf("-F", "src/**/*.kt")
	jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

semver {
    noGitPush = false
}

tasks.withType<KotlinCompile>() {

    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)

        freeCompilerArgs.addAll(listOf(
            "-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn"
        ))
    }
}

jib {
    from {
        image = "registry://ghcr.io/bell-sw/liberica-openjdk-alpine:21"
    }

    to {
        image = "ghcr.io/lostcities-cloud/${project.name}:${project.version}"
        tags = mutableSetOf("latest", "${project.version}")
        auth {
            username = System.getenv("GH_USER")
            password = System.getenv("GH_TOKEN")
        }
    }

}

dependencyCheck {
    failBuildOnCVSS = 11f
    failOnError = false
    formats = mutableListOf("JUNIT", "HTML", "JSON")
    data {
        directory = "${rootDir}/owasp"
    }
    //suppressionFiles = ['shared-owasp-suppressions.xml']
    analyzers {
        assemblyEnabled = false
    }
    nvd {
        apiKey = System.getenv("NVD_KEY")
        delay = 16000
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}



tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}


