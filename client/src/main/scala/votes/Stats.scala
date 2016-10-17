package votes

import org.scalajs.dom
import org.scalajs.dom.raw.{ Event, HTMLInputElement }
import votes.Aggregate

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.scalajs.js.JSConverters._
import scala.util.Try

object Stats extends JSApp {
  val canvas = dom.document.getElementById("myChart")
  val chart = new BubbleChart(canvas)
  var oldAgg: Aggregate = null  // for change tracking.

  def main(): Unit = {
    val rfactorElement = dom.document.getElementById("rfactor").asInstanceOf[HTMLInputElement]
    rfactorElement.onkeyup = { e: Event => updateItems(oldAgg.items) }

    def updateItems(items: Seq[Aggregate.Item]) = {
      val rfactorValue = Try(rfactorElement.value.toInt).toOption.getOrElse(15)
      val dataPoints = items.sortBy(-_.count).map {
        i => DataPoint(i.language.value, i.age, i.count * rfactorValue)
      }.toJSArray
      chart.updateData(dataPoints)
    }

    dom.window.setInterval(() => {
      val x = new dom.XMLHttpRequest()
      x.open("GET", "/statsData")
      x.responseType = "arraybuffer"
      x.send()
      x.onload = {
        event: Event =>
          import js.typedarray._
          val response = x.response.asInstanceOf[ArrayBuffer]
          val agg = Aggregate.parseFrom(new Int8Array(response).toArray)
          if (agg != oldAgg) {
            updateItems(agg.items)
            oldAgg = agg
          }
      }
    }, 1000.0)
  }
}
