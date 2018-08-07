package se.chimps.rpc.util

import java.util.Base64

import org.json4s.DefaultFormats
import org.json4s.native.Serialization.{read, write}

trait Json {
	implicit val format = DefaultFormats

	protected def fromJson[T](bytes:Array[Byte])(implicit tag:Manifest[T]):T = {
		read[T](new String(bytes, "utf-8"))
	}

	protected def toJson[T](data: T):Array[Byte] = {
		write(data).getBytes("utf-8")
	}

	protected def base64Encode(bytes:Array[Byte]):String = Base64.getEncoder.encodeToString(bytes)

	protected def base64Decode(data:String):Array[Byte] = Base64.getDecoder.decode(data.getBytes("utf-8"))

}
