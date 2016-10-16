package controllers

import java.nio.ByteBuffer
import java.util.UUID

import com.amazonaws.services.kinesis.producer.{ KinesisProducer, KinesisProducerConfiguration }
import com.redis.RedisClient
import controllers.ProtoUtils.protoParser
import play.api.mvc._
import votes.votes.Vote


class RootController extends Controller {
  val kinesis = new KinesisProducer(new KinesisProducerConfiguration().setRegion("us-west-2"))

  def index = Action {
    Ok(views.html.index()).withSession("userid" -> UUID.randomUUID().toString)
  }

  def submit = Action(protoParser[Vote]) {
    request =>
      val userId = request.session("userid")
      val voteWithSession = request.body.update(_.userId := userId)

      kinesis.addUserRecord(
        "proto-demo2", userId,
        ByteBuffer.wrap(request.body.toByteArray))

      Ok("foo")
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
