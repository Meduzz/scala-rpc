package se.chimps.rpc

import java.util.concurrent.TimeUnit

import io.nats.client.{Connection, Dispatcher, Message => Msg}
import se.chimps.rpc.util.JsonUtil

import scala.concurrent.{ExecutionContext, Future}
import io.nats.client.Implicits._

import scala.concurrent.duration.Duration

class RPC(conn:Connection) {
	private var subz:Map[String, Dispatcher] = Map()

	def request(to:String, message: Message, timeout:Int)(implicit ec:ExecutionContext):Future[Message] = {
		asConn(conn).request(to, JsonUtil.stringify(message), "utf-8")(ec, Duration(timeout.toLong, TimeUnit.SECONDS))
  		.map(msg => JsonUtil.parse[Message](new String(msg.getData, "utf-8")))
	}

	def trigger(to:String, message: Message):Unit = {
		asConn(conn).publish(to, JsonUtil.stringify(message), "utf-8")
	}

	def handler(topic:String, queue:Option[String], handler: Handler):Unit = {
		val dispatcher = queue match {
			case Some(q) => asConn(conn).subscribe(topic, q, wrap(handler))
			case None => asConn(conn).subscribe(topic, wrap(handler))
		}

		subz = subz ++ Map(topic -> dispatcher)
	}

	def remove(topic:String):Unit = {
		subz.get(topic) match {
			case Some(d) => d.unsubscribe(topic)
			case None =>
		}
	}

	private def wrap(handler:Handler):(Msg => Unit) = { msg =>
		val ctx = new NatsContext(conn, msg)
		handler(ctx)
	}
}

object RPC {
	def apply(conn:Connection):RPC = new RPC(conn)
}

case class Message(metadata:Map[String, String], body:String) {
	def withBody[T](body:T):Message = copy(body = JsonUtil.stringify(body))
	def withPlainBody(body:String):Message = copy(body = body)
	def readBody[T]()(implicit m:Manifest[T]):T = JsonUtil.parse[T](body)
	def addHeader(key:String, value:String):Message = copy(metadata = metadata ++ Map(key -> value))
}

object MessageBuilder {
	def newSuccess(headers:Map[String, String] = Map()):Message = Message(headers ++ Map("result" -> "success"), "")
	def newError(msg:String):Message = Message(Map("result" -> "error"), JsonUtil.stringify(ErrorDTO(msg)))
	def newEmpty():Message = Message(Map(), "")
}

case class ErrorDTO(msg:String)

trait Context {
	def body():Message
	def reply(message: Message):Unit
	def forward(to:String, message: Message):Unit
	def trigger(to:String, message:Message):Unit
	def request(to:String, message: Message, timeout:Int)(implicit ec:ExecutionContext):Future[Message]
}

trait Handler extends (Context => Unit)

class NatsContext(val conn:Connection, msg:Msg) extends Context {
	override def body():Message = JsonUtil.parse[Message](new String(msg.getData, "utf-8"))

	override def reply(message:Message):Unit = asConn(conn).publish(msg.getReplyTo, JsonUtil.stringify(message), "utf-8")

	override def forward(to:String, message:Message):Unit = asConn(conn).publish(to, JsonUtil.stringify(message), "utf-8")

	override def trigger(to:String, message:Message):Unit = asConn(conn).publish(to, JsonUtil.stringify(message), "utf-8")

	override def request(to:String, message:Message, timeout:Int)(implicit ec:ExecutionContext):Future[Message] = {
		asConn(conn).request(to, JsonUtil.stringify(message), "utf-8")(ec, Duration(timeout.toLong, TimeUnit.SECONDS))
  		.map(rep => JsonUtil.parse[Message](new String(rep.getData, "utf-8")))
	}
}