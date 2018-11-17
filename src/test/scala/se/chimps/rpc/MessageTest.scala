package se.chimps.rpc

import org.scalatest.FunSuite
import se.chimps.rpc.util.JsonUtil

class MessageTest extends FunSuite {

	test("body with curves handles like a charm") {
		val original = Test("Bosse", 86)
		val msg = MessageBuilder.newEmpty()
	  	.withBody(original)

		assert(msg.body == """{"name":"Bosse","age":86}""")

		val bodyJson = msg.readBody[Test]()

		assert(original == bodyJson)

		val msgJson = JsonUtil.stringify(msg)

		assert(msgJson == """{"metadata":{},"body":{"name":"Bosse","age":86}}""")

		val msg2 = JsonUtil.parse[Message](msgJson)

		assert(JsonUtil.parse[Test](msg2.body) == original)
	}

	test("a more simple version of body also works") {
		val msg = MessageBuilder.newEmpty()
	  	.withPlainBody("Hello world!")

		assert(msg.body == "Hello world!")

		val jsonMsg = JsonUtil.stringify(msg)

		assert(jsonMsg == """{"metadata":{},"body":"Hello world!"}""")

		val msg2 = JsonUtil.parse[Message](jsonMsg)

		assert(msg2 == msg)
	}

}

case class Test(name:String, age:Int)