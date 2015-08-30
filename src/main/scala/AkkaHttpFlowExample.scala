import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.IOException
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.math._
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.server.RequestContext
import scala.concurrent.Future
import akka.http.scaladsl.model.HttpMethods._
import scala.concurrent.duration._
import scala.concurrent.Await


object AkkaHttpFlowExample extends App {

	implicit val system=ActorSystem("akka-http-flow-example")
	sys.addShutdownHook({ system.shutdown() })

	implicit val executor= system.dispatcher
	implicit val materializer = ActorMaterializer()

	val httpReqHandler:HttpRequest=>HttpResponse={
			  case HttpRequest(GET, Uri.Path("/"), _, _, _) =>
			                    
				                HttpResponse(entity = HttpEntity(MediaTypes.`text/html`, "<html><body>Please access with query param eg: count?id=2</body></html>"))
 
			  case HttpRequest(GET, Uri.Path("/ping"), _, _, _) => HttpResponse(entity = "PONG!")
			 
			 
              case d@HttpRequest(_,Uri.Path("/count"),_,_,_) =>  
			                         val id=d.uri.query.get("id").getOrElse(1)
									 val result=customFlow(id.asInstanceOf[String].toInt)
                                     HttpResponse(status=StatusCodes.OK,entity="Akka Http/Stream Processing Result Is:"+result)
									 
			  case _:HttpRequest => HttpResponse(status=StatusCodes.OK,entity="Access with query param eg: count?id=2")					 
	}
	
	
	val serverSource:Source[Http.IncomingConnection,Future[Http.ServerBinding]]=Http().bind(interface="0.0.0.0",port=9000)
	val serverBinding:Future[Http.ServerBinding]=serverSource.to(Sink.foreach{  conn =>
																		conn handleWithSyncHandler httpReqHandler
																}).run()   
									
    def customFlow(n:Int)={


		val step1 =  Flow[Int].map(_+1)
		val step2 =  Flow[Int].map(_+2)
		val step3 =  Flow[Int].map(_+3)

        
		val broadCastAndZipFlow= Flow(){
                     implicit b =>
					 import FlowGraph.Implicits._
					 
					 val bcast=b.add(Broadcast[Int](3))
					 val zip=b.add(ZipWith[Int,Int,Int,Int]((i1,i2,i3)=>i1+i2+i3))
					 
					 bcast.out(0)~>step1~>zip.in0
					 bcast.out(1)~>step2~>zip.in1
					 bcast.out(2)~>step3~>zip.in2
					 
					 (bcast.in,zip.out)

          } 

        println("Running the Akka Http/Stream custom flow")
		//1. Create a stream of 1..n numbers
		//2. Pass the stream to the custom flow
		//3. Custom Flow: Stream -> BroadCast -> Zip -> Sink -> Fold->Future[Tuple]-> Await and extract
		//4. For async, we can use the asynchandler
		val result=broadCastAndZipFlow.runWith(Source(1 to n),Sink.fold[Int, Int](0)(_ + _))._2
		Await.result(result,Duration(100, MILLISECONDS))

  }
									
}
