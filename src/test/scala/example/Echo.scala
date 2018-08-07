package example

import se.chimps.rpc.{Request, Response, RpcBuilder}

import scala.concurrent.ExecutionContext.global
import scala.sys.ShutdownHookThread

object Echo {
	def main(args:Array[String]): Unit = {
		val rpc = RpcBuilder.nats()
	  	.worker("echo", echo)(global)
			.build(global)

		ShutdownHookThread {
			rpc.stop()
		}

		println("Running...")
		rpc.start()
	}

	def echo(req:Request):Response = {
		Response(200, Map("Content-Type" -> "text/plain"), req.body)
	}
}
