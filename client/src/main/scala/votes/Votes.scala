package votes

import org.scalajs.dom
import org.scalajs.dom.raw.Event
import votes.Vote
import votes.Vote.Language

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import scala.util.Try
import scalatags.JsDom.all._

object VotesMain extends js.JSApp {
  def main(): Unit = {
    val elem = input(`type` := "number", `class` := "form-control").render

    val statusElem = p().render

    var vote = Vote()

    def updateAge(e: Event) = {
      vote = vote.update(_.age := Try(elem.value.toInt).toOption.getOrElse(0))
    }

    def updateLanguage(language: Language) = {
      vote = vote.update(_.language := language)
    }

    def submitForm(e: Event) = {
      e.preventDefault()
      val xhr = new dom.XMLHttpRequest()
      import js.JSConverters._
      val t = new Uint8Array(vote.toByteArray.toJSArray)
      xhr.open("POST", "/submit")
      xhr.onload = {
        e: Event  =>
          statusElem.textContent = xhr.responseText
          if (xhr.status == 200) {
            dom.document.getElementById("submitBtn").setAttribute("disabled", "true")
          }
      }
      xhr.send(t)
    }

    elem.onkeyup = updateAge _
    elem.onchange = updateAge _

    val e = form(
      label("What is your least favorite language?"),

      for (v <- Language.values.filterNot(_.isUnknown)) yield div(
        `class` := "radio",
        label(
          input(
            `type` := "radio",
            name := "language",
            onclick := {_: Event => updateLanguage(v) }), v.name
        )),

      div(`class` := "form-group",
        label("How old are you?"),
        elem),

      statusElem,

      input(`type` := "submit", id := "submitBtn",
        `class` := "btn btn-default btn-primary",
        onclick := submitForm _)
    )

    dom.document.getElementById("target").appendChild(e.render)
  }
}
