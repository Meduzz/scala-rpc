package example

import io.nats.client.Nats
import se.chimps.rpc
import se.chimps.rpc.{Context, Handler, MessageBuilder}

object Echo {

	val echo:Handler = { ctx:Context =>
		val msg = ctx.body()

		val reply = MessageBuilder.newSuccess()
	  	.withPlainBody(msg.body)

		ctx.reply(reply)
	}

	def main(args:Array[String]): Unit = {
		val conn = Nats.connect()

		val server = rpc.RPC(conn)
		server.handler("echo", None, echo)
	}

}
