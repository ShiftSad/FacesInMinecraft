plugins {
	id("fabric-loom") version "1.10.5"
	id("maven-publish")
	id("org.jetbrains.kotlin.jvm") version "2.1.20"
}

version = project.property("mod_version")!!
group = project.property("maven_group")!!

base {
	archivesName.set(project.property("archives_base_name") as String)
}

repositories {

}

loom {
	splitEnvironmentSourceSets()

	mods {
		create("facemod") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}
}

dependencies {
	// To change the versions see the gradle.properties file
	"minecraft"("com.mojang:minecraft:${project.property("minecraft_version")}")
	"mappings"("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
	"modImplementation"("net.fabricmc:fabric-loader:${project.property("loader_version")}")

	// Fabric API. This is technically optional, but you probably want it anyway.
	"modImplementation"("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
	"modImplementation"("net.fabricmc:fabric-language-kotlin:${project.property("fabric_kotlin_version")}")

	implementation("com.github.sarxos:webcam-capture:0.3.12")
}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(mapOf("version" to inputs.properties["version"]))
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
	kotlinOptions {
		jvmTarget = "21"
	}
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	inputs.property("archivesName", project.base.archivesName.get())

	from("LICENSE") {
		rename { "${it}_${inputs.properties["archivesName"]}" }
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = project.property("archives_base_name") as String
			from(components["java"])
		}
	}
	repositories {
		// Add repositories to publish to here.
		// Notice: This block does NOT have the same function as the block in the top level.
		// The repositories here will be used for publishing your artifact, not for
		// retrieving dependencies.
	}
}
