import com.redis.RedisClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010.{ ConsumerStrategies, KafkaUtils, LocationStrategies }
import votes.votes.Aggregate.Item
import votes.votes.Vote.Language
import votes.votes.{ Aggregate, Vote }

object Aggregator {
  val CheckpointDir = "/tmp/chkp"

  def createContext() = {
    val conf = new SparkConf().setAppName("mything").setMaster("local[8]")
    conf.set("spark.streaming.receiver.writeAheadLog.enable", "true")

    val ssc = new StreamingContext(conf, Seconds(1))
    ssc.checkpoint(CheckpointDir)

    val votes = {
      val kafkaParams = Map[String, Object](
        "bootstrap.servers" -> "localhost:32768",
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[ByteArrayDeserializer],
        "group.id" -> "demo",
        "enable.auto.commit" -> (false: java.lang.Boolean))
      val str = KafkaUtils.createDirectStream[String, Array[Byte]](ssc, LocationStrategies.PreferConsistent,
        ConsumerStrategies.Subscribe[String, Array[Byte]](Array("votes"), kafkaParams))

      str.map {
        (cr: ConsumerRecord[String, Array[Byte]]) =>
          Vote.parseFrom(cr.value())
      }
    }

    val votesByLanguageAge = votes.map {
      v => ((v.language, v.age / 10 * 10), v)
    }

    val t = votesByLanguageAge.updateStateByKey[Int] {
      (votes: Seq[Vote], currentStat: Option[Int]) => Some(currentStat.getOrElse(0) + votes.size)
    }

    t.print()

    t.foreachRDD {
      r: RDD[((Language, Int), Int)] =>
        val items: Seq[Item] = r.collect.toSeq.map {
          case ((language, age), count) =>
            Aggregate.Item().update(
              _.language := language,
              _.age := age,
              _.count := count)
        }
        val a = Aggregate(items = items)
        val c = new RedisClient()
        c.set(s"stats", a.toByteArray)
        c.disconnect
    }
    ssc
  }


  def main(args: Array[String]): Unit = {
    val ssc = StreamingContext.getOrCreate(CheckpointDir, () => createContext())

    ssc.start()
    ssc.awaitTermination()
  }

}
