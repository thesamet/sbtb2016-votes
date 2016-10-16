import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.amazonaws.services.kinesis.model.Record
import com.redis.RedisClient
import org.apache.spark._
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming._
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.kinesis.KinesisUtils
import votes.votes.Aggregate.Item
import votes.votes.{ Aggregate, Vote }
import votes.votes.Vote.Language

object ProtoDemo {

  def messageHandler(record: Record): Vote = {
    try {
      Vote.parseFrom(record.getData.array())
    } catch {
      case e: com.google.protobuf.InvalidProtocolBufferException => Vote()
    }
  }

  def createContext() = {
    val conf = new SparkConf().setAppName("mything").setMaster("local[8]")
    conf.set("spark.kryo.registrationRequired", "true")
    conf.set("spark.serializer", "org.apache.spark.serializer.JavaSerializer")

    conf.set("spark.streaming.receiver.writeAheadLog.enable", "true")

    val ssc = new StreamingContext(conf, Seconds(1))
    ssc.checkpoint("/tmp/chkp")

    val votes: ReceiverInputDStream[Vote] = KinesisUtils.createStream(
      ssc, "ProtoDemo7", "proto-demo2", "https://kinesis.us-west-2.amazonaws.com",
      "us-west-2", InitialPositionInStream.TRIM_HORIZON, Seconds(60),
      StorageLevel.MEMORY_AND_DISK_2, messageHandler)

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


  /*
  val votesByUserId = votes.map {
    v => (v.userId, v)
  }.groupByKey()
  */

  def main(args: Array[String]): Unit = {
    val ssc = StreamingContext.getOrCreate("/tmp/chkp", () => createContext())

    ssc.start()
    ssc.awaitTermination()
  }

}
