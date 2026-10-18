plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "frb.axeron.adb"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=none"
                // Hanya kompilasi native untuk perangkat Android (ARM).
                abiFilters += listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = project.file("src/main/cpp/CMakeLists.txt")
//            version = "3.31.0"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        prefab = true
    }

}

dependencies {
    implementation(project(":api"))
    implementation(libs.core.ktx)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.androidx.lifecycle.livedata.core.ktx)

    implementation(libs.boringssl)
    implementation(libs.libcxx)

    implementation(libs.rikka.hidden.compat)
    compileOnly(libs.rikka.hidden.stub)
}