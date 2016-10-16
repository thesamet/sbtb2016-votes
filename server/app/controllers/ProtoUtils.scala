package controllers

import com.trueaccord.scalapb.{ GeneratedMessage, GeneratedMessageCompanion, Message }
import play.api.mvc.{ BodyParser, BodyParsers }
import concurrent.ExecutionContext.Implicits.global

object ProtoUtils extends BodyParsers {

  def protoParser[A <: GeneratedMessage with Message[A]](implicit cmp: GeneratedMessageCompanion[A]): BodyParser[A] = parse.raw(maxLength = 200).map {
    t =>
      t.asBytes().map(_.toArray).map(cmp.parseFrom _).getOrElse(throw new RuntimeException("Could not parse"))
  }

}
