package se.chimps.rpc

import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import se.chimps.rpc.providers.local.{LocalClient, LocalServer}
import scala.concurrent.ExecutionContext.Implicits.global

class RpcTest extends FunSuite with ScalaFutures {

	val reverse = Worker({ msg:Message =>
		MessageBuilder.newSuccess()
			.withPlainBody(msg.body.reverse)
	})

	val server = LocalServer()
	server.registerWorker("reverse", reverse)

	test("dev experience is at least descent") {
		val client = LocalClient(server)

		val res = client.request("reverse", MessageBuilder.newEmpty()
			.withPlainBody("Hello world!"))

		whenReady(res) { msg =>
			assert(msg.body == "Hello world!".reverse)
			assert(msg.metadata("result") == "success")
		}
	}
}
