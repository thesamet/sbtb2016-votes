package controllers

import com.redis.RedisClient
import play.api.mvc.{ Action, Controller }

class StatsController extends Controller {
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
