import com.redis.RedisClient
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ ByteArrayDeserializer, StringDeserializer }
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.{ ConsumerStrategies, KafkaUtils, LocationStrategies }
import votes.votes.Vote.Language
import votes.votes.{ Aggregate, Vote }

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

    val votesAsBytes = KafkaUtils.createDirectStream[String, Array[Byte]](
      ssc, LocationStrategies.PreferConsistent,
      ConsumerStrategies.Subscribe[String, Array[Byte]](Array("votes"), kafkaParams))

    val votes: DStream[Vote] = votesAsBytes.map {
      (cr: ConsumerRecord[String, Array[Byte]]) =>
        Vote.parseFrom(cr.value())
    }

    ssc
  }


  def main(args: Array[String]): Unit = {
    val ssc = StreamingContext.getOrCreate(CheckpointDir, () => createContext())

    ssc.start()
    ssc.awaitTermination()
  }

}
