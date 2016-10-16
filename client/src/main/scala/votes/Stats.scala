package votes

import org.scalajs.dom
import org.scalajs.dom.raw.{ Element, Event }

import scala.scalajs.js.JSApp
import scala.scalajs.js
import scalatags.JsDom.all._
import votes.Aggregate

import scala.scalajs.js.annotation.JSName

object Stats extends JSApp {
  def main(): Unit = {
    val e = div("hello")
    val canvas = dom.document.getElementById("myChart")
    js.Dictionary
    new js.Array()
    val c = new Chart(canvas, js.Dynamic.literal("type" -> "bubble",
      "data" -> js.Dynamic.literal(
        "yLabels" -> js.Array(votes.Vote.Language.values.map(_.name): _*),
        "datasets" ->
          js.Array(js.Dynamic.literal(
            "label" -> "First dataset",
          "data" -> js.Array(
            js.Dynamic.literal("x" -> 1, "y" -> "30", "r" -> 15)
          )
        ))
    )))

    println(canvas.getClass)

    val x = new dom.XMLHttpRequest()
    x.open("GET", "/statsData")
    x.responseType = "arraybuffer"
    x.send()
    x.onload = {
      event: Event =>
        import js.typedarray._
        val response = x.response.asInstanceOf[ArrayBuffer]
        val agg = Aggregate.parseFrom(new Int8Array(response).toArray)
        println(agg.items.map(_.language))
    }

  }

}

@JSName("Chart")
@js.native
class Chart(e: Element, options: js.Object) extends js.Object
