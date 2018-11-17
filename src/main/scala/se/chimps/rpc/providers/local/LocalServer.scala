package se.chimps.rpc.providers.local

import se.chimps.rpc._

import scala.concurrent.{ExecutionContext, Future}

class LocalServer(implicit val ec:ExecutionContext) extends RpcServer {

	private var workers:Map[String, Worker] = Map()
	private var eventers:Map[String, Eventer] = Map()

	override def registerWorker(topic:String, func:Worker):Unit = {
		workers = workers ++ Map(topic -> func)
	}

	override def registerEventer(topic:String, func:Eventer):Unit = {
		eventers = eventers ++ Map(topic -> func)
	}

	override def start(settings:Option[Settings] = None):Unit = {}

	override def stop():Unit = {}

	def request(topic:String, msg:Message):Future[Message] = {
		workers.get(topic) match {
			case Some(worker) => worker(msg)
			case None => Future(MessageBuilder.newError("Invalid topic, no worker found"))
		}
	}

	def trigger(topic:String, msg:Message):Unit = {
		eventers.get(topic) match {
			case Some(eventer) => eventer(msg)
			case None =>
		}
	}
}

object LocalServer {
	def apply()(implicit ec:ExecutionContext):LocalServer = new LocalServer()
}