import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.1.+"
    id("io.spring.dependency-management") version "1.1.+"
    // id("org.graalvm.buildtools.native") version "0.10.+"
    id("org.jetbrains.dokka") version "1.6.10"
	id("com.google.cloud.tools.jib") version "3.4.3"
	kotlin("jvm") version "2.0.+"
	kotlin("plugin.spring") version "2.0.+"
}

group = "io.dereknelson.lostcities"
version = "0.0.1-SNAPSHOT"

repositories {

	maven {
		url = uri("https://maven.pkg.github.com/lostcities-cloud/lostcities-models")
		credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
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

dependencies {
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation(project(":lostcities-common"))
    implementation(project(":lostcities-models"))

    implementation("org.springframework.boot:spring-boot-devtools")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.micrometer:micrometer-registry-otlp")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.6.0")
    implementation("org.springdoc:springdoc-openapi-kotlin:1.8.0")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

	ktlint("com.pinterest:ktlint:0.44.0") {
		attributes {
			attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
		}
	}
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.10")
	//implementation("com.fasterxml.jackson.core:jackson-core:2.5.2")

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
        image = "registry://bellsoft/liberica-openjdk-alpine:21.0.4-9-cds"
	}
	to {
		image = "ghcr.io/lostcities-cloud/${project.name}:latest"
		auth {
			username = System.getenv("GH_ACTOR")
    		password = System.getenv("GH_TOKEN")
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

