import java.io.{OutputStreamWriter, BufferedWriter, PrintWriter}

/**
  * Created by Caleb Prior on 23-Dec-15.
  */
class Group (name:String, id:Int) {
  val groupName:String = name
  val groupId:Int = id
  var clients:List[Client] = List()

  def addClient(newClient:Client):Unit = {
    clients = newClient :: clients
  }

  def removeClient(clientId:Int):Unit = {
    var newClients:List[Client] = List()

    for(c <- clients){
      if(c.joinId != clientId){
        newClients = c :: newClients
      }
    }

    clients = newClients
  }

  def hasClient(clientId:Int):Boolean = {
    clients.nonEmpty && clients.exists(c => c.joinId == clientId)
  }

  def sendMessage(sender:Client, message:String):Unit ={
    val msg = "CHAT:" + groupId + "\nCLIENT_NAME:" + sender.handle + "\nMESSAGE:" + message + "\n"

    for(c <- clients){
      val bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(c.socket.getOutputStream)))
      bufferOut.println(msg)
      bufferOut.flush()
    }
  }
}
