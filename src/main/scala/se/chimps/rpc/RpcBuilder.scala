package se.chimps.rpc

import io.nats.client.{Connection, Message, Nats, Options}
import se.chimps.rpc.util.Json

import scala.concurrent.{ExecutionContext, Future}

trait RpcBuilder {
	def worker(topic:String, handler:(Request)=>Response)(implicit ec:ExecutionContext):RpcBuilder
	def workerAsync(topic:String, handler:(Request)=>Future[Response])(implicit ec:ExecutionContext):RpcBuilder
	def workerGroup(topic:String, group:Option[String], handler:(Request)=>Response)(implicit ec:ExecutionContext):RpcBuilder
	def workerGroupAsync(topic:String, group:Option[String], handler:(Request)=>Future[Response]):RpcBuilder
	def event(topic:String, handler:(Request)=>Unit)(implicit ec:ExecutionContext):RpcBuilder
	def eventAsync(topic:String, handler:(Request)=>Future[Unit])(implicit ec:ExecutionContext):RpcBuilder
	def eventGroup(topic:String, group:Option[String], handler:(Request)=>Unit)(implicit ec:ExecutionContext):RpcBuilder
	def eventGroupAsync(topic:String, group:Option[String], handler:(Request)=>Future[Unit]):RpcBuilder
	def raw(topic:String, handler:(Message)=>Unit)(implicit ec:ExecutionContext):RpcBuilder
	def rawAsync(topic:String, handler:(Message)=>Future[Unit])(implicit ec:ExecutionContext):RpcBuilder
	def rawGroup(topic:String, group:Option[String], handler:(Message)=>Unit)(implicit ec:ExecutionContext):RpcBuilder
	def rawGroupAsync(topic:String, group:Option[String], handler:(Message)=>Future[Unit]):RpcBuilder
	def rawResponse(topic:String, handler:(Message)=>Message)(implicit ec:ExecutionContext):RpcBuilder
	def rawResponseAsync(topic:String, handler:(Message)=>Future[Message])(implicit ec:ExecutionContext):RpcBuilder
	def rawResponseGroup(topic:String, group:Option[String], handler:(Message)=>Message)(implicit ec:ExecutionContext):RpcBuilder
	def rawResponseGroupAsync(topic:String, group:Option[String], handler:(Message)=>Future[Message]):RpcBuilder
	def connection():Connection
	def build(ec:ExecutionContext):Rpc
}

object RpcBuilder {
	def nats():RpcBuilder = {
		Builder(Nats.connect())
	}

	def nats(url:String):RpcBuilder = {
		Builder(Nats.connect(url))
	}

	def nats(options:Options):RpcBuilder = {
		Builder(Nats.connect(options))
	}

	def nats(connection: Connection):RpcBuilder = {
		Builder(connection)
	}
}

case class Builder(nats:Connection,
                   workers:Seq[WorkerMeta] = Seq(),
                   events:Seq[EventMeta] = Seq(),
                   raw:Seq[RawMeta] = Seq(),
                   rawResponse:Seq[RawResponseMeta] = Seq()) extends RpcBuilder {

	override def worker(topic:String, handler:Request => Response)(implicit ec:ExecutionContext):RpcBuilder = {
		workerGroup(topic, None, handler)
	}

	override def workerAsync(topic:String, handler:Request => Future[Response])(implicit ec:ExecutionContext):RpcBuilder = {
		workerGroupAsync(topic, None, handler)
	}

	override def workerGroup(topic:String, group:Option[String], handler:Request => Response)(implicit ec:ExecutionContext):RpcBuilder = {
		workerGroupAsync(topic, group, asFuture(handler))
	}

	override def workerGroupAsync(topic:String, group:Option[String], handler:Request => Future[Response]):RpcBuilder = {
		val workers = this.workers ++ Seq(WorkerMeta(topic, group, handler))
		copy(workers = workers)
	}

	override def event(topic:String, handler:Request => Unit)(implicit ec:ExecutionContext):RpcBuilder = {
		eventGroup(topic, None, handler)
	}

	override def eventAsync(topic:String, handler:Request => Future[Unit])(implicit ec:ExecutionContext):RpcBuilder = {
		eventGroupAsync(topic, None, handler)
	}

	override def eventGroup(topic:String, group:Option[String], handler:Request => Unit)(implicit ec:ExecutionContext):RpcBuilder = {
		eventGroupAsync(topic, group, asFuture(handler))
	}

	override def eventGroupAsync(topic:String, group:Option[String], handler:Request => Future[Unit]):RpcBuilder = {
		val events = this.events ++ Seq(EventMeta(topic, group, handler))
		copy(events = events)
	}

	override def raw(topic:String, handler:Message => Unit)(implicit ec:ExecutionContext):RpcBuilder = {
		rawGroup(topic, None, handler)
	}

	override def rawAsync(topic:String, handler:Message => Future[Unit])(implicit ec:ExecutionContext):RpcBuilder = {
		rawGroupAsync(topic, None, handler)
	}

	override def rawGroup(topic:String, group:Option[String], handler:Message => Unit)(implicit ec:ExecutionContext):RpcBuilder = {
		rawGroupAsync(topic, group, asFuture(handler))
	}

	override def rawGroupAsync(topic:String, group:Option[String], handler:Message => Future[Unit]):RpcBuilder = {
		val raw = this.raw ++ Seq(RawMeta(topic, group, handler))
		copy(raw = raw)
	}

	override def rawResponse(topic:String, handler:Message => Message)(implicit ec:ExecutionContext):RpcBuilder = {
		rawResponseGroup(topic, None, handler)
	}

	override def rawResponseAsync(topic:String, handler:Message => Future[Message])(implicit ec:ExecutionContext):RpcBuilder = {
		rawResponseGroupAsync(topic, None, handler)
	}

	override def rawResponseGroup(topic:String, group:Option[String], handler:Message => Message)(implicit ec:ExecutionContext):RpcBuilder = {
		rawResponseGroupAsync(topic, group, asFuture(handler))
	}

	override def rawResponseGroupAsync(topic:String, group:Option[String], handler:Message => Future[Message]):RpcBuilder = {
		val raw = this.rawResponse ++ Seq(RawResponseMeta(topic, group, handler))
		copy(rawResponse = rawResponse)
	}

	override def build(ec:ExecutionContext):Rpc = Rpc(this, ec)

	override def connection():Connection = nats

	private def asFuture[T,K](handler:(T)=>K)(implicit ec:ExecutionContext):(T)=>Future[K] = (req) => Future(handler(req))
}

case class WorkerMeta(topic:String, group:Option[String], handler:(Request)=>Future[Response])
case class EventMeta(topic:String, group:Option[String], handler:(Request)=>Future[Unit])
case class RawMeta(topic:String, group:Option[String], handler:(Message)=>Future[Unit])
case class RawResponseMeta(topic:String, group:Option[String], handler:(Message)=>Future[Message])

/**
	* @param metadata a collection of the headers that was sent in the request.
	* @param body the body in the request, if any. (important it's Base64-encoded!)
	*/
case class Request(metadata:Map[String, String], body:String) extends Json {
	/**
		* @return returns the body without the base64 encoding, ie the raw bytes.
		*/
	def rawBody():Array[Byte] = base64Decode(body)
	/**
		* @tparam T the return type.
		* @return returns T.
		*/
	def bodyFromJson[T]()(implicit tag:Manifest[T]):T = {
		val bytes = rawBody()
		fromJson[T](bytes)
	}
}

/**
	* @param code the response code the proxy should write (HTTP-status codes)
	* @param metadata any headers the proxy should write.
	* @param body the body the proxy should write (important it should be base64 encoded!)
	*/
case class Response(code:Int, metadata:Map[String, String] = Map(), body:String = "") extends Json {
	def withHeader(key:String, value:String):Response = copy(metadata = metadata ++ Map(key -> value))
	def withHeaders(headers:Map[String, String]):Response = copy(metadata = metadata ++ headers)
	/**
		* @param body a raw byte array, THAT WILL BE Base64 ENCODED.
		* @return returns a new copy of self.
		*/
	def withBody(body:Array[Byte]): Response = copy(body = base64Encode(body))
	/**
		* @param body a body that will be encoded to json, and then base64 encoded.
		* @tparam T can be any anyref.
		* @return returns a copy of self.
		*/
	def withBody[T](body:T):Response = {
		val contentType = Map("Content-Type" -> "application/json")
		val data = toJson(body)

		copy(metadata = metadata ++ contentType, body = base64Encode(data))
	}
}