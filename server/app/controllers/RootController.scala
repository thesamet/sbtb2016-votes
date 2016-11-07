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
      Ok("Not implemented yet!")
  }
}
