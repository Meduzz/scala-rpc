package se.chimps.rpc.providers.local

import se.chimps.rpc.{Message, RpcClient}

import scala.concurrent.Future

class LocalClient(private val server:LocalServer) extends RpcClient {
	override def request(topic:String, msg:Message):Future[Message] = server.request(topic, msg)

	override def trigger(topic:String, msg:Message):Unit = server.trigger(topic, msg)
}

object LocalClient {
	def apply(server:LocalServer):LocalClient = new LocalClient(server)
}
