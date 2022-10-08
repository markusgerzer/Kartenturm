
import com.soywiz.klock.*
import com.soywiz.korge.box2d.*
import com.soywiz.korge.input.*
import com.soywiz.korge.scene.*
import com.soywiz.korge.tween.*
import com.soywiz.korge.view.*
import com.soywiz.korim.color.*
import com.soywiz.korim.text.*
import com.soywiz.korio.async.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.interpolation.*
import org.jbox2d.dynamics.*
import zoom.*

class CardScene : Scene() {
    private lateinit var scheme: List<SolidRect>
    private lateinit var cards: List<SolidRect>
    private lateinit var clickBlocker: SolidRect
    private lateinit var ground: SolidRect
    private lateinit var scoreView: Text
    private lateinit var zoomComponent: ZoomComponent

    val isRunning get() = clickBlocker.parent == null
    var score = 0
        private set(value) {
            field = value
            scoreView.text = score.toString()
        }

    override suspend fun SContainer.sceneInit() {
        scoreView = text(score.toString()) {
            color = Colors.BLACK
            alignTopToTopOf(this@sceneInit, 32)
            alignRightToRightOf(this@sceneInit, 32)
        }
        text("Score:  ") {
            color = Colors.BLACK
            alignTopToTopOf(scoreView)
            alignRightToLeftOf(scoreView)
        }

        scheme = buildScheme()

        val shadows = List(scheme.size) {
            solidRect(CARD_SHADOW_SIZE_X, CARD_SHADOW_SIZE_Y, CARD_SHADOW_COLOR)
        }

        cards = List(scheme.size) { card(shadow = shadows[it]).xy(400 - it, 50 + it) }

        ground = solidRect(3 * width, 10.0, GROUND_COLOR)
            .position(-width, 500.0)
            .registerBodyWithFixture(type = BodyType.STATIC, friction = GROUND_FRICTION)

        clickBlocker = solidRect(width, height, Colors.TRANSPARENT_WHITE) {
            onClick { println("Click Blocked") }
        }
    }

    override suspend fun SContainer.sceneMain() {
        moveCardsToScheme()
        installTowerCrashChecker()
        runGame()
    }

    private fun SContainer.runGame() {
        println("Game is running")
        clickBlocker.removeFromParent()
        zoomComponent = addZoomComponent(ZoomComponent(this))
    }
    private fun SContainer.stopGame() {
        if (isRunning) {
            println("Game stopped")
            while (scale > 1.0) zoomComponent.zoomOut()
            removeZoomComponent(zoomComponent)
            clickBlocker.addTo(this)
        }
    }
    private fun SContainer.gameOver() {
        stopGame()
        val text = text("Game Over") {
            color = Colors.DARKORANGE
            alignment = TextAlignment.MIDDLE_CENTER
            centerOnStage()
        }
        launch {
            tween(text::fontSize[96.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
            tween(text::fontSize[64.0], time = 1.seconds, easing = Easing.EASE_IN_OUT)
        }
        clickBlocker.onClick {
            sceneContainer.changeTo<CardScene>()
        }
    }

    private fun SContainer.schemeCard(block: SolidRect.() -> Unit) =
        solidRect(CARD_SIZE_X, CARD_SIZE_Y, Colors.YELLOW.withA(0x00), block)

    private fun SContainer.buildScheme(): List<SolidRect> = buildList {
        val x0 = (width - 12 * CARD_SIZE_X_D) * .5
        var x = x0
        var y = 500.0 //ground.y

        for (i in 0 until 6) {
            for (j in i until 6) {
                add(schemeCard {
                    rotation = ANGEL_RIGHT
                    xy(x, y)
                    x += CARD_SIZE_X_D
                })
                add(schemeCard {
                    rotation = ANGEL_LEFT
                    x += CARD_SIZE_X_D
                    xy(x, y)
                })
            }
            y -= CARD_SIZE_Y_D
            x = x0 + (i + 2) * CARD_SIZE_X_D - 0.5 * CARD_SIZE_X
            for (j in i + 1 until 6 step 2) {
                add(schemeCard {
                    xy(x, y)
                    x += 4 * CARD_SIZE_X_D
                })
            }
            y -= CARD_SIZE_Y
            x = x0 + (i + 4) * CARD_SIZE_X_D - 0.5 * CARD_SIZE_X
            for (j in i + 2 until 6 step 2) {
                add(schemeCard {
                    xy(x, y)
                    x += 4 * CARD_SIZE_X_D
                })
            }
            y -= CARD_SIZE_Y
            x = x0 + (i + 1) * CARD_SIZE_X_D
        }
    }

    private fun SContainer.card(
        width: Double = CARD_SIZE_X,
        height: Double = CARD_SIZE_Y,
        color: RGBA = CARD_COLOR,
        angle: Angle = 0.degrees,
        shadow: SolidRect? = null
    ): SolidRect = solidRect(width, height, color) {
        rotation = angle
        val thisCard = this
        val clickGetter = this@card.solidRect(width, height * 15, Colors.BLUE.withAd(.0)){
            anchor(.0, .5)
            onClick {
                thisCard.removeFromParent()
                shadow?.removeFromParent()
                removeFromParent()
                score++
            }
        }
        addUpdater {
            clickGetter.x = x
            clickGetter.y = y
            clickGetter.rotation = rotation
            if (shadow != null) {
                shadow.y = y + 1.0
                shadow.x = x + 1.0
                shadow.rotation = rotation
            }
        }
    }

    private suspend fun moveCardsToScheme() {
        var idx = 0
        for (i in 0 until 6) {
            for (j in i until 6) {
                moveCardToScheme(idx, 2)
                idx += 2
            }
            for (j in i + 1 until 6 step 2) {
                moveCardToScheme(idx++)
            }
            for (j in i + 2 until 6 step 2) {
                moveCardToScheme(idx++)
            }
        }
    }

    private suspend fun moveCardToScheme(idx: Int, n: Int = 1) {
        val args = Array<V2<*>>(3 * n) {
            val i = idx + it / 3
            return@Array when (it % 3) {
                0 -> cards[i]::rotation[scheme[i].rotation]
                1 -> cards[i]::x[scheme[i].x]
                2 -> cards[i]::y[scheme[i].y]
                else -> throw IllegalStateException()
            }
        }
        cards[idx].tween(*args, time = .25.seconds, easing = Easing.EASE_IN_OUT)
        repeat(n) {
            cards[idx + it]
                .registerBodyWithFixture(
                    type = BodyType.DYNAMIC, density = CARD_DENSITY, friction = CARD_FRICTION
                )
        }
    }

    private fun SContainer.installTowerCrashChecker(testLastNCards: Int = 2) {
        for (card in cards.takeLast(testLastNCards)) {
            val marker = solidRect(width, 1.0, Colors.RED.withA(0x00)).centerYOn(card)
            card.addUpdater {
                if (isRunning && windowBounds.y > marker.windowBounds.y) {
                    println("Tower crashed")
                    gameOver()
                }
            }
        }
    }

    companion object {
        val GROUND_COLOR = Colors.DARKSLATEBLUE
        val CARD_COLOR = Colors.LIGHTGRAY
        val CARD_SHADOW_COLOR = Colors.DARKSLATEGRAY
        const val GROUND_FRICTION = 0.2
        const val CARD_DENSITY = .3
        const val CARD_FRICTION = 1.5
        const val CARD_SIZE_X = 75.0
        const val CARD_SIZE_Y = 1.0
        const val CARD_SHADOW_SIZE_X = CARD_SIZE_X
        const val CARD_SHADOW_SIZE_Y = CARD_SIZE_Y
        val ANGEL_RIGHT = (-70).degrees
        val ANGEL_LEFT = (-110).degrees
        val CARD_SIZE_X_D: Double
        val CARD_SIZE_Y_D: Double
        init {
            val rect = SolidRect(CARD_SIZE_X, CARD_SIZE_Y, Colors.WHITE).apply { rotation = ANGEL_RIGHT }
            CARD_SIZE_X_D = rect.windowBounds.width + 1.0
            CARD_SIZE_Y_D = rect.windowBounds.height + 1.0
        }
    }
}
