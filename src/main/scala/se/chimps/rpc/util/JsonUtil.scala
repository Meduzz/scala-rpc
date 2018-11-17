package se.chimps.rpc.util

import java.util.Base64

import org.json4s
import org.json4s.native.JsonMethods.{compact, render}
import org.json4s.{DefaultFormats, JString, JValue}
import org.json4s.native.JsonParser.{parse => jsonParse}
import org.json4s.native.Serialization.{read, write}
import se.chimps.rpc.Message

object JsonUtil {
	// Lets call this solution a mvp?
	private val messageBodySerializer = json4s.FieldSerializer[Message](
		serializer = {
			case ("body", body:String) => {
				if (body.startsWith("{")) {
					Some("body", jsonParse(body))
				} else {
					Some("body", body)
				}
			}
		}, deserializer = {
			case ("body", body:JString) => ("body", body)
			case ("body", body:JValue) => {
				("body", JString(compact(render(body))))
			}
		}
	)

	implicit val format = DefaultFormats + messageBodySerializer

	def parse[T](json:String)(implicit tag:Manifest[T]):T = {
		read[T](json)
	}

	def stringify[T](data: T):String = {
		write(data)
	}

	protected def base64Encode(bytes:Array[Byte]):String = Base64.getEncoder.encodeToString(bytes)

	protected def base64Decode(data:String):Array[Byte] = Base64.getDecoder.decode(data.getBytes("utf-8"))

}
