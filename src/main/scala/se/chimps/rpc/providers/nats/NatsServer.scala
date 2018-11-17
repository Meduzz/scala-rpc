package se.chimps.rpc.providers.nats

import io.nats.client._
import se.chimps.rpc.util.JsonUtil
import se.chimps.rpc.{Eventer, MessageBuilder, RpcServer, Settings, Worker, Message => RpcMessage}

import scala.concurrent.ExecutionContext

class NatsServer(private val conn:Connection)(implicit ec:ExecutionContext) extends RpcServer {

	private var workers = Map[String, Worker]()
	private var eventers = Map[String, Eventer]()
	private var subscriptions = Map[String, Dispatcher]()

	override def registerWorker(topic:String, func:Worker):Unit = {
		workers = workers ++ Map(topic -> func)
	}

	override def registerEventer(topic:String, func:Eventer):Unit = {
		eventers = eventers ++ Map(topic -> func)
	}

	override def start(settings:Option[Settings] = None):Unit = {
		val settingz = settings.getOrElse(Settings(false, ""))

		val subs = workers.map {
			case (topic, worker) => {
				val dispatcher = conn.createDispatcher(asWorker(worker))

				if (settingz.queue) {
					dispatcher.subscribe(topic, settingz.name)
				} else {
					dispatcher.subscribe(topic)
				}

				(topic, dispatcher)
			}
		}

		subscriptions = subs ++ eventers.map {
			case (topic, eventer) => {
				val dispatcher = conn.createDispatcher(asEventer(eventer))

				if (settingz.queue) {
					dispatcher.subscribe(topic, settingz.name)
				} else {
					dispatcher.subscribe(topic)
				}

				(topic, dispatcher)
			}
		}

		println("RPC server started.")
	}

	override def stop():Unit = {
		subscriptions.foreach {
			case (topic, dispatcher) => dispatcher.unsubscribe(topic)
		}

		subscriptions = Map()
		conn.close()
	}

	private def asWorker(func:Worker):MessageHandler = {
		(natsMsg:Message) => {
			val json = new String(natsMsg.getData, "utf-8")
			val rpcMsg = JsonUtil.parse[RpcMessage](json)
			func(rpcMsg)
		    .recover {
				  case e:Throwable => MessageBuilder.newError(e.getMessage)
			  }
		  	.foreach(retMsg => {
				  conn.publish(natsMsg.getReplyTo, JsonUtil.stringify(retMsg).getBytes("utf-8"))
			  })
		}
	}

	private def asEventer(func:Eventer):MessageHandler = {
		(natsMsg:Message) => {
			val json = new String(natsMsg.getData, "utf-8")
			val rpcMsg = JsonUtil.parse(json)
			func(rpcMsg)
		}
	}
}

object NatsServer {
	def apply()(implicit ec:ExecutionContext): NatsServer = {
		new NatsServer(Nats.connect())
	}

	def apply(url:String)(implicit ec:ExecutionContext):NatsServer = {
		new NatsServer(Nats.connect(url))
	}

	def apply(options:Options)(implicit ec:ExecutionContext):NatsServer = {
		new NatsServer(Nats.connect(options))
	}
}
