extra.apply {
    set("kotlinVersion", "2.1.10")
    set("shadowVersion", "8.3.0")
    set("runPaperVersion", "2.3.1")
    set("fabricLoomVersion", "1.7.1")
}

allprojects {
    group = "codes.shiftmc"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}