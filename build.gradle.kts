import com.soywiz.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
    id("com.soywiz.korlibs.kotlin-source-dependency-gradle-plugin") version "0.1.1"
}

sourceDependencies {
    source("https://github.com/markusgerzer/lib.git")
}

korge {
	id = "com.example.example"
	supportBox2d()
// To enable all targets at once

	//targetAll()

// To enable targets based on properties/environment variables
	//targetDefault()

// To selectively enable targets

	targetJvm()
	targetJs()
	targetDesktop()
	//targetIos()
	targetAndroidIndirect() // targetAndroidDirect()
}
