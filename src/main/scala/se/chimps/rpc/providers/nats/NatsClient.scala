package se.chimps.rpc.providers.nats

import java.util.concurrent.TimeUnit

import io.nats.client.{Connection, Nats, Options}
import se.chimps.rpc.util.JsonUtil
import se.chimps.rpc.{Message, MessageBuilder, RpcClient}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class NatsClient(private val conn:Connection) extends RpcClient {

	override def request(topic:String, msg:Message):Future[Message] = {
		val result = Promise[Message]()

		val bytes = JsonUtil.stringify(msg).getBytes("utf-8")
		Try(conn.request(topic, bytes)
	  	.get(3L, TimeUnit.SECONDS)) match {
				case Success(msg) => {
					result.success(JsonUtil.parse[Message](new String(msg.getData, "utf-8")))
				}
				case Failure(e) => {
					result.success(MessageBuilder.newError(e.getMessage))
				}
		}

		result.future
	}

	override def trigger(topic:String, msg:Message):Unit = {
		val bytes = JsonUtil.stringify(msg).getBytes("utf-8")
		conn.publish(topic, bytes)
	}

}

object NatsClient {
	def apply(): NatsClient = {
		new NatsClient(Nats.connect())
	}

	def apply(url:String):NatsClient = {
		new NatsClient(Nats.connect(url))
	}

	def apply(options:Options):NatsClient = {
		new NatsClient(Nats.connect(options))
	}
}
