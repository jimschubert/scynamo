package scynamo

import java.util

import cats.data.EitherNec
import scynamo.generic.{GenericScynamoEnumDecoder, GenericScynamoEnumEncoder}
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

trait ScynamoCodec[A] extends ScynamoEncoder[A] with ScynamoDecoder[A] { self =>
  def imap[B](f: A => B)(g: B => A): ScynamoCodec[B] = new ScynamoCodec[B] {
    override def decode(attributeValue: AttributeValue): EitherNec[ScynamoDecodeError, B] = self.decode(attributeValue).map(f)
    override def encode(value: B): AttributeValue                                         = self.encode(g(value))
  }

  def itransform[B](f: EitherNec[ScynamoDecodeError, A] => EitherNec[ScynamoDecodeError, B])(g: B => A): ScynamoCodec[B] =
    new ScynamoCodec[B] {
      override def decode(attributeValue: AttributeValue): EitherNec[ScynamoDecodeError, B] = f(self.decode(attributeValue))

      override def encode(value: B): AttributeValue = self.encode(g(value))
    }
}

object ScynamoCodec {
  def apply[A](implicit codec: ScynamoCodec[A]): ScynamoCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](
      implicit scynamoEncoder: ScynamoEncoder[A],
      scynamoDecoder: ScynamoDecoder[A]
  ): ScynamoCodec[A] =
    new ScynamoCodec[A] {
      override def encode(value: A): AttributeValue                                         = scynamoEncoder.encode(value)
      override def decode(attributeValue: AttributeValue): EitherNec[ScynamoDecodeError, A] = scynamoDecoder.decode(attributeValue)
    }
}

trait ObjectScynamoCodec[A] extends ObjectScynamoEncoder[A] with ObjectScynamoDecoder[A] with ScynamoCodec[A] { self =>
  override def imap[B](f: A => B)(g: B => A): ObjectScynamoCodec[B] = new ObjectScynamoCodec[B] {
    override def decode(attributeValue: AttributeValue): EitherNec[ScynamoDecodeError, B]             = self.decode(attributeValue).map(f)
    override def encodeMap(value: B): java.util.Map[String, AttributeValue]                           = self.encodeMap(g(value))
    override def decodeMap(value: util.Map[String, AttributeValue]): EitherNec[ScynamoDecodeError, B] = self.decodeMap(value).map(f)
  }
}

object ObjectScynamoCodec {
  def apply[A](implicit codec: ObjectScynamoCodec[A]): ObjectScynamoCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](
      implicit encoder: ObjectScynamoEncoder[A],
      decoder: ObjectScynamoDecoder[A]
  ): ObjectScynamoCodec[A] =
    new ObjectScynamoCodec[A] {
      override def encodeMap(a: A): java.util.Map[String, AttributeValue]                               = encoder.encodeMap(a)
      override def decodeMap(value: util.Map[String, AttributeValue]): EitherNec[ScynamoDecodeError, A] = decoder.decodeMap(value)
    }
}

trait ScynamoEnumCodec[A] extends ScynamoCodec[A]

object ScynamoEnumCodec {
  def apply[A](implicit codec: ScynamoEnumCodec[A]): ScynamoEnumCodec[A] = codec

  implicit def fromEncoderAndDecoder[A](
      implicit encoder: GenericScynamoEnumEncoder[A],
      decoder: GenericScynamoEnumDecoder[A]
  ): ScynamoEnumCodec[A] = new ScynamoEnumCodec[A] {
    override def encode(value: A): AttributeValue = encoder.encode(value)

    override def decode(attributeValue: AttributeValue): EitherNec[ScynamoDecodeError, A] = decoder.decode(attributeValue)
  }
}
