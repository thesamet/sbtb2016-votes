package controllers

import java.util.UUID

import controllers.ProtoUtils.protoParser
import org.apache.kafka.clients.producer.{ KafkaProducer, ProducerRecord }
import org.apache.kafka.common.serialization.{ ByteArraySerializer, StringSerializer }
import play.api.mvc._
import votes.votes.Vote

class RootController extends Controller {
  val kafka: KafkaProducer[String, Array[Byte]] = {
    import scala.collection.JavaConverters._
    new KafkaProducer(Map[String, Object](
      "bootstrap.servers" -> "localhost:9092"
    ).asJava, new StringSerializer, new ByteArraySerializer)
  }

  def index = Action {
    request =>
      Ok(views.html.index()).withSession(
        request.session + ("userid" -> UUID.randomUUID().toString))
  }

  def submit = Action(protoParser[Vote]) {
    request =>
      val vote: Vote = request.body

      if (request.session.get("voted").isDefined) {
        Conflict("Already voted.")
      } else if (vote.age > 120 || vote.age <= 0) {
        BadRequest("Age not in range.")
      } else if (vote.language.isUnknown) {
        BadRequest("Please select a language.")
      } else {
        val userId = request.session("userid")
        val voteWithSession = vote.update(_.userId := userId)

        kafka.send(new ProducerRecord("votes", "", voteWithSession.toByteArray))
        Ok("Thank you!").withSession(request.session + ("voted" -> "1"))
      }
  }
}
