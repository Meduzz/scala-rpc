package se.chimps.rpc

import io.nats.client.{Dispatcher, Message, MessageHandler}
import se.chimps.rpc.util.Json

import scala.concurrent.{ExecutionContext, Future}

case class Rpc(builder:Builder, executionContext: ExecutionContext) extends Json {
	implicit val ec = executionContext

	private var subscriptions:Seq[Subscription] = Seq()

	def start():Unit = {
		builder.workers.foreach(w => {
			val d = builder.nats.createDispatcher(worker(w.handler))
			val s = w.group match {
				case Some(g) => d.subscribe(w.topic, g)
				case None => d.subscribe(w.topic)
			}
			subscriptions = subscriptions ++ Seq(Subscription(w.topic, s))
		})

		builder.events.foreach(e => {
			val d = builder.nats.createDispatcher(event(e.handler))
			val s = e.group match {
				case Some(g) => d.subscribe(e.topic, g)
				case None => d.subscribe(e.topic)
			}

			subscriptions = subscriptions ++ Seq(Subscription(e.topic, s))
		})

		builder.raw.foreach(r => {
			val d = builder.nats.createDispatcher(raw(r.handler))
			val s = r.group match {
				case Some(g) => d.subscribe(r.topic, g)
				case None => d.subscribe(r.topic)
			}

			subscriptions = subscriptions ++ Seq(Subscription(r.topic, s))
		})

		builder.rawResponse.foreach(r => {
			val d = builder.nats.createDispatcher(rawResponse(r.handler))
			val s = r.group match {
				case Some(g) => d.subscribe(r.topic, g)
				case None => d.subscribe(r.topic)
			}

			subscriptions = subscriptions ++ Seq(Subscription(r.topic, s))
		})
	}

	def stop():Unit = {
		subscriptions.foreach(d => d.dispatcher.unsubscribe(d.topic))
		subscriptions = Seq()
	}

	private def worker(func:(Request)=>Future[Response]):MessageHandler = { msg =>
		val req = fromJson[Request](msg.getData)
		func(req)
	    .recover({
		    case e:Throwable => Response(500, Map(), e.getMessage)
	    })
			.map(toJson)
	  	.map(json => builder.nats.publish(msg.getReplyTo, json))
	}

	private def event(func:(Request)=>Future[Unit]):MessageHandler = { msg =>
		val req = fromJson[Request](msg.getData)
		func(req)
	}

	private def raw(func:(Message)=>Future[Unit]):MessageHandler = { msg =>
		func(msg)
	}

	private def rawResponse(func:(Message)=>Future[Message]):MessageHandler = { msg =>
		func(msg)
	  	.foreach(msg => builder.nats.publish(msg.getSubject, msg.getData))
	}
}

case class Subscription(topic:String, dispatcher:Dispatcher)