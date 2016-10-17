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
      "bootstrap.servers" -> "10.0.1.201:32768"
    ).asJava, new StringSerializer, new ByteArraySerializer)
  }

  def index = Action {
    request =>
      Ok(views.html.index()).withSession(
        request.session + ("userid" -> UUID.randomUUID().toString))
  }

  val allowMultiple = true

  def submit = Action(protoParser[Vote]) {
    request =>
      if (request.session.get("voted").isEmpty || allowMultiple) {
        val userId = request.session("userid")
        val voteWithSession = request.body.update(_.userId := userId)

        kafka.send(new ProducerRecord("moishe", "xyz", voteWithSession.toByteArray))
        Ok("").withSession(request.session + ("voted" -> "1"))
      } else {
        Conflict("Already voted.")
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
