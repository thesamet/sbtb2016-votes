package votes

import org.scalajs.dom.raw.Element

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSName

class BubbleChart(canvas: Element) {
  private val LanguageMaxValue = votes.Vote.Language.values.map(_.value).max

  private val dataSets = js.Array(
    literal(
      "label" -> "Live dataset",
      "data" -> js.Array()
    ))

  private def config = literal(
    "type" -> "bubble",
    "options" -> literal(
      "maintainAspectRatio" -> false,
      "scales" -> literal(
        "xAxes" -> js.Array(literal(
          "ticks" -> literal(
            "min" -> 0,
            "max" -> (1 + LanguageMaxValue),
            "stepSize" -> 1,
            "callback" -> {
              (value: Number) =>
                val intValue = value.intValue()
                if (intValue >= 1 && intValue <= LanguageMaxValue) votes.Vote.Language.fromValue(intValue).name else ""
            }
          )
        )),
        "yAxes" -> js.Array(literal(
          "scaleLabel" -> literal(
            "display" -> true,
            "labelString" -> "Age"
          ),
          "ticks" -> literal(
            "min" -> -10,
            "stepSize" -> 10
          )
        ))
      )
    ),
    "data" -> literal(
      "datasets" -> dataSets
    )
  )

  val c = new Chart(canvas, config)

  def updateData(items: Seq[DataPoint]) = {
    dataSets(0).backgroundColor = (0 to items.size).map(i => s"hsla(${360 * i / items.size}, 100%, 50%, 0.8)").toJSArray
    dataSets(0).data = items.toJSArray
    c.update()
  }
}

@js.native
trait DataPoint extends js.Object {
  var x: Int = js.native
  var y: Int = js.native
  var r: Int = js.native
}

object DataPoint {
  def apply(x: Int, y: Int, r: Int): DataPoint = {
    js.Dynamic.literal("x" -> x, "y" -> y, "r" -> r).asInstanceOf[DataPoint]
  }
}

@JSName("Chart")
@js.native
class Chart(e: Element, options: js.Object) extends js.Object {
  def update(): Unit = js.native
}

