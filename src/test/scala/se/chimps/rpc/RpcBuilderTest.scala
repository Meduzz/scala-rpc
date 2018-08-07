package se.chimps.rpc

import example.Echo
import org.scalatest.FunSuite
import se.chimps.rpc.util.Json

class RpcBuilderTest extends FunSuite with Json {

	test("asdf") {
		val data = Test("Asdf", 99)
		val req = Request(Map(), base64Encode(toJson(data)))
		val res = Echo.echo(req)

		val reqBody = req.bodyFromJson[Test]()
		val resBody = fromJson[Test](base64Decode(res.body))

		assert(reqBody.age == 99)
		assert(reqBody.name == "Asdf")
		assert(resBody.age == 99)
		assert(resBody.name == "Asdf")
	}

}

case class Test(name:String, age:Int)