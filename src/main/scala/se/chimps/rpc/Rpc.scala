package se.chimps.rpc

import se.chimps.rpc.util.JsonUtil

import scala.concurrent.{ExecutionContext, Future}

trait RpcServer {
	def registerWorker(topic:String, func:Worker)
	def registerEventer(topic:String, func:Eventer)
	def start(opts:Option[Settings] = None)
	def stop()
}

trait RpcClient {
	def request(topic:String, msg:Message):Future[Message]
	def trigger(topic:String, msg:Message):Unit
}

case class Message(metadata:Map[String, String], body:String) {
	def withBody[T](body:T):Message = copy(body = JsonUtil.stringify(body))
	def withPlainBody(body:String):Message = copy(body = body)
	def readBody[T]()(implicit m:Manifest[T]):T = JsonUtil.parse[T](body)
	def addHeader(key:String, value:String):Message = copy(metadata = metadata ++ Map(key -> value))
}

object MessageBuilder {
	def newSuccess(headers:Map[String, String] = Map()):Message = Message(headers ++ Map("result" -> "success"), "")
	def newError(msg:String):Message = Message(Map("result" -> "error"), JsonUtil.stringify(ErrorDTO(msg)))
	def newEmpty():Message = Message(Map(), "")
}

case class ErrorDTO(msg:String)

trait Worker extends (Message => Future[Message])
trait Eventer extends (Message => Unit)

object Worker {
	def apply(func:(Message)=>Future[Message]):Worker = {
		(msg:Message) => func(msg)
	}

	def apply(func:(Message)=>Message)(implicit ec:ExecutionContext):Worker = {
		(msg:Message) => Future(func(msg))
	}
}

object Eventer {
	def apply(func:(Message)=>Unit):Eventer = {
		(msg:Message) => func(msg)
	}
}

case class Settings(queue:Boolean, name:String)