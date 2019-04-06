package se.chimps.rpc

import io.nats.client.Nats
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext.Implicits.global

class RpcTest extends FunSuite with ScalaFutures {

	val reverse:Handler = { ctx:Context =>
		val msg = ctx.body()

		val reply = MessageBuilder.newSuccess()
			.withPlainBody(msg.body.reverse)

		ctx.reply(reply)
	}

	val rpc = RPC(Nats.connect())
	rpc.handler("reverse", None, reverse)

	test("dev experience is at least descent") {
		val res = rpc.request("reverse", MessageBuilder.newEmpty()
			.withPlainBody("Hello world!"), 1)

		whenReady(res) { msg =>
			assert(msg.body == "Hello world!".reverse)
			assert(msg.metadata("result") == "success")
		}
	}
}
