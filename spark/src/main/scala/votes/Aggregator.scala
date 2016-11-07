import com.redis.RedisClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ ConsumerStrategies, KafkaUtils, LocationStrategies }
import votes.agg.SurveyResult
import votes.votes.Vote.Language
import votes.votes.Vote

object Aggregator {
  val CheckpointDir = "/tmp/chkp"

  def createContext() = {
    val conf = new SparkConf().setAppName("myaggr").setMaster("local[8]")
    conf.set("spark.streaming.receiver.writeAheadLog.enable", "true")

    val ssc = new StreamingContext(conf, Seconds(1))
    ssc.checkpoint(CheckpointDir)

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[ByteArrayDeserializer],
      "group.id" -> "demo",
      "enable.auto.commit" -> (false: java.lang.Boolean))

    // Stream of votes from Kafka as bytes
    val votesAsBytes = KafkaUtils.createDirectStream[String, Array[Byte]](
      ssc, LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, Array[Byte]](Array("votes"), kafkaParams))

    // Parse them into Vote case class.
    val votes: DStream[Vote] = votesAsBytes.map {
      (cr: ConsumerRecord[String, Array[Byte]]) =>
        Vote.parseFrom(cr.value())
    }

    // Round down the age to a multiple of ten. The key is
    // (language, age) the value is the count.
    val votesByLanguageAge = votes.map {
      v => ((v.language, v.age / 10 * 10), v)
    }

    // Aggregate the vote count for each key.
    val voteCounts = votesByLanguageAge.updateStateByKey[Int] {
      (votes: Seq[Vote], currentStat: Option[Int]) =>
        Some(currentStat.getOrElse(0) + votes.size)
    }

    // Save in Redis.
    voteCounts.foreachRDD {
      r: RDD[((Language, Int), Int)] =>
        val buckets = r.collect.map {
          case ((language, age), count) =>
            SurveyResult.Bucket().update(
              _.language := language,
              _.age := age,
              _.count := count)
        }
        val a = SurveyResult(buckets = buckets)
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
