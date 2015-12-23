import java.net.Socket

/**
  * Created by Caleb Prior on 23-Dec-15.
  */
class Client (clientHandle:String, clientId:Int, clientSocket:Socket){
  val handle:String = clientHandle
  val joinId:Int = clientId
  var socket: Socket = clientSocket
}
