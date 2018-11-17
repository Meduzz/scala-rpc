package example

import se.chimps.rpc.providers.nats.NatsServer
import se.chimps.rpc.{Message, MessageBuilder, Worker}

import scala.concurrent.ExecutionContext.Implicits.global

object Echo {

	val echo = Worker({ msg:Message =>
		MessageBuilder.newSuccess()
	  	.withPlainBody(msg.body)
	})

	def main(args:Array[String]): Unit = {
		val server = NatsServer()
		server.registerWorker("echo", echo)
		server.start()
	}

}
