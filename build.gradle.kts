import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.0"
	id("io.spring.dependency-management") version "1.1.5"
}
group = "com.empresa"
version = "0.0.1-SNAPSHOT"
kotlin {
	jvmToolchain(17)
}
repositories {
	mavenCentral()
}
dependencies {
// ── WebFlux ─────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-webflux")
// ── R2DBC ────────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("org.postgresql:r2dbc-postgresql")
	runtimeOnly("org.postgresql:postgresql")
// ── Kotlin ───────────────────────────────────────
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
// ── Validação ─────────────────────────────────────
	implementation("org.springframework.boot:spring-boot-starter-validation")
// ── Testes ───────────────────────────────────────
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.projectreactor:reactor-test")
	testImplementation("io.mockk:mockk:1.13.10")
	testImplementation("com.ninja-squad:springmockk:4.0.2")
}
tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}
tasks.withType<Test> {
	useJUnitPlatform()
}
