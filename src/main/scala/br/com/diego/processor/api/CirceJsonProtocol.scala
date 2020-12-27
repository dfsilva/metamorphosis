package br.com.diego.processor.api

import java.sql.Timestamp
import java.text.SimpleDateFormat
import io.circe.Decoder.Result
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Decoder, Encoder, HCursor, Json}

trait CirceJsonProtocol {

  implicit val TimestampFormat: Encoder[Timestamp] with Decoder[Timestamp] = new Encoder[Timestamp] with Decoder[Timestamp] {
    override def apply(a: Timestamp): Json = Encoder.encodeString.apply(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(a))

    override def apply(c: HCursor): Result[Timestamp] = Decoder.decodeString.map(s => new Timestamp(new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(s).getTime)).apply(c)
  }

  implicit val DateFormat: Encoder[java.sql.Date] with Decoder[java.sql.Date] = new Encoder[java.sql.Date] with Decoder[java.sql.Date] {
    override def apply(a: java.sql.Date): Json = Encoder.encodeString.apply(new SimpleDateFormat("dd/MM/yyyy").format(a))

    override def apply(c: HCursor): Result[java.sql.Date] = Decoder.decodeString.map(s => new java.sql.Date(new SimpleDateFormat("dd/MM/yyyy").parse(s).getTime)).apply(c)
  }

//  implicit val AnyDecoder: Encoder[Any] = new Encoder[Any] {
//    override def apply(a: Any): Json = Encoder.encodeString.apply(a.toString)
//  }

//  implicit val wsMessageDecoder: Decoder[WsMessage] = deriveDecoder[WsMessage]

}

