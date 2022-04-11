import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.6.3"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.jetbrains.dokka") version "1.6.10"

	kotlin("jvm") version "1.6.10"
	kotlin("plugin.spring") version "1.6.10"
}

group = "io.dereknelson.lostcities"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_16

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

dependencies {
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	implementation("io.dereknelson.lostcities-cloud:lostcities-models:1.0-SNAPSHOT")

    implementation("org.springframework.boot:spring-boot-devtools")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

    implementation("org.springframework.cloud:spring-cloud-starter-consul-discovery:3.1.0")
    implementation("com.google.cloud:spring-cloud-gcp-starter:2.0.8")
    implementation("com.google.cloud:spring-cloud-gcp-starter-secretmanager:2.0.8")

	ktlint("com.pinterest:ktlint:0.44.0") {
		attributes {
			attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
		}
	}
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.10")
	//implementation("com.fasterxml.jackson.core:jackson-core:2.5.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

val outputDir = "${project.buildDir}/reports/ktlint/"
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

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "16"
	}
}

tasks.bootRun {
	if (project.hasProperty("debug_jvm")) {
		jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5935")
	}
}

tasks.getByName<BootBuildImage>("bootBuildImage") {
    imageName = "ghcr.io/lostcities-cloud/${project.name}"
    isPublish = true
    environment = mapOf(
        "BP_JVM_VERSION" to "17.*",
        "BPL_DEBUG_ENABLED" to "true",
        "JAVA_TOOL_OPTIONS" to "-Xquickstart -Xshareclasses:cacheDir=/cache"
    )
    builder = "paketobuildpacks/builder:base"
    buildpacks = listOf(
        "gcr.io/paketo-buildpacks/eclipse-openj9",
        "paketo-buildpacks/java",
        "gcr.io/paketo-buildpacks/spring-boot"
    )

    docker {
        publishRegistry {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
            email = "lostcities@dereknelson.io"
        }
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}

