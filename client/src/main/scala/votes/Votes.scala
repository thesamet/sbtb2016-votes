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
    val elem = input(`type` := "number", name := "What is your age?").render

    var vote = Vote()

    def updateAge(e: Event) = {
      vote = vote.update(_.age := Try(elem.value.toInt).toOption.getOrElse(0))
    }

    def updateLanguage(language: Language) = {
      vote = vote.update(_.language := language)
    }

    elem.onkeyup = updateAge _
    elem.onchange = updateAge _

    def submitForm() = {
      val xhr = new dom.XMLHttpRequest()
      import js.JSConverters._
      val t = new Uint8Array(vote.toByteArray.toJSArray)
      xhr.open("POST", "/submit")
      xhr.send(t)
    }

    val e = div(
      p("What is your least favorite language?"),

      for (v <- Language.values) yield div(`class` := "radio",
        label(
          input(`type` := "radio", name := "language", onclick := {_: Event => updateLanguage(v) }), v.name
        )),

      p("What year were you born? ", elem),

      p(input(`type` := "submit", onclick := submitForm _))
    )

    dom.document.getElementById("target").appendChild(e.render)

    val v = votes.Vote(age = 35, language = votes.Vote.Language.PHP)
  }
}
