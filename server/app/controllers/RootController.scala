package controllers

import java.util.UUID

import com.redis.RedisClient
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

  val allowMultiple = false

  def submit = Action(protoParser[Vote]) {
    request =>
      if (request.session.get("voted").isDefined && !allowMultiple) {
        Conflict("Already voted.")
      } else if (request.body.age > 120 || request.body.age <= 0) {
        BadRequest("Age not in range.")
      } else if (request.body.language.isUnknown) {
        BadRequest("Please select a language.")
      } else {
        val userId = request.session("userid")
        val voteWithSession = request.body.update(_.userId := userId)

        kafka.send(new ProducerRecord("votes", "xyz", voteWithSession.toByteArray))
        Ok("Thank you!").withSession(request.session + ("voted" -> "1"))
      }
  }

  def statsIndex = Action {
    Ok(views.html.stats())
  }

  def statsData = Action {
    import com.redis.serialization.Parse.Implicits._
    val c = new RedisClient()
    try {
      Ok(c.get[Array[Byte]]("stats").get)
    } finally {
      c.disconnect
    }
  }
}
