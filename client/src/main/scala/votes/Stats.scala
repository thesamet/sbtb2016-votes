package votes

import org.scalajs.dom
import org.scalajs.dom.raw.{ Event, HTMLInputElement }

import scala.scalajs.js
import scala.scalajs.js.JSApp
import scala.util.Try
import agg.SurveyResult
import scala.scalajs.js.JSConverters._

object Stats extends JSApp {
  val canvas = dom.document.getElementById("myChart")
  val chart = new BubbleChart(canvas)
  var oldAgg: SurveyResult = null  // for change tracking.

  def main(): Unit = {
    val rfactorElement = dom.document.getElementById("rfactor").asInstanceOf[HTMLInputElement]
    rfactorElement.onkeyup = { e: Event => updateBuckets(oldAgg.buckets) }

    def updateBuckets(items: Seq[SurveyResult.Bucket]) = {
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
          val agg = SurveyResult.parseFrom(new Int8Array(response).toArray)
          if (agg != oldAgg) {
            updateBuckets(agg.buckets)
            oldAgg = agg
          }
      }
    }, 1000.0)
  }
}
