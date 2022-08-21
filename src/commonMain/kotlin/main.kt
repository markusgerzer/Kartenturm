import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korgw.*
import com.soywiz.korim.color.*
import com.soywiz.korinject.*
import com.soywiz.korma.geom.*
import com.soywiz.korui.layout.*
import kotlin.reflect.*

suspend fun main() = Korge(Korge.Config(module = ConfigModule))

object ConfigModule : Module() {
    override val size = SizeInt(512, 512)
    override val quality = GameWindow.Quality.PERFORMANCE
    override val bgcolor = Colors.GHOSTWHITE
    override val scaleMode = ScaleMode.SHOW_ALL
    override val clipBorders = false
    override val scaleAnchor = Anchor.BOTTOM
    override val mainScene : KClass<out Scene> = CardScene::class

    override suspend fun AsyncInjector.configure() {
        mapPrototype { CardScene() }
    }
}

