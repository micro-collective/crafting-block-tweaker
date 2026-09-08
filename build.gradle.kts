plugins {
    id("st.evening.mc.prelude.build") version "1.0.0"
    id("st.evening.kt.invokecontrol") version "2.3.20-0.1.0"
    id("maven-publish")
}

group = "st.evening.mc.cbtweaker"
version = "1.0.0"

preludeBuild {
    modId = "cbtweaker"
    mod {
        constantsClass = "CbtConsts"
    }
    minecraft {
        mcVersion = "1.12.2"
        deobf {
            mappingsChannel = "stable"
            mappingsVersion = "39"
        }
    }
    presets {
        prelude {
            enable = true
        }
    }
}

repositories {
    mavenLocal()
    maven {
        name = "CurseMaven"
        url = uri("https://www.cursemaven.com/")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "CleanroomMC"
        url = uri("https://repo.cleanroommc.com/releases")
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
    }
}

dependencies {
    api("st.evening.kt.invokecontrol:kt-invoke-control-lib:0.1.0")
    compileOnlyApi("mezz.jei:jei_1.12.2:4.15.0.291:api")
    implementation(rfg.deobf("curse.maven:mekanism-268560:2835175")) // 9.8.3.390
}
