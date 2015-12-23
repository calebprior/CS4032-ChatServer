import java.io._
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

trait ChatSeverTrait {
  def shutdown:Unit
  def getUniqueId:Int

  def getGroup(groupName:String):Group
  def getGroup(groupId:Int):Group
  def addGroup(newGroup:Group):Unit
  def groupExists(groupName:String):Boolean
  def groupExists(groupId:Int):Boolean

  def getClient(clientName:String):Client
  def addClient(newClient:Client):Unit
  def clientExists(clientName:String):Boolean
  def removeClientFromAll(client:Client):Unit
}

/**
  * Created by Caleb Prior on 23-Dec-15.
  */
object ChatServer extends ChatSeverTrait{
  var running: Boolean = true
  var poolSize: Int = 15
  var port: Int = -1
  var pool: ExecutorService = null
  var serverSocket:ServerSocket = null

  var uniqueId = 0
  var groups:List[Group] = List()
  var clients:List[Client] = List()

  def main (args: Array[String]) {
    Setup(args(0))
    println("CHAT SERVER: Started on port " + port + " with thread pool size of: " + poolSize)

    while(running){
      try{
        val socket = serverSocket.accept()
        println("CHAT SERVER: Connection Received")
        pool.execute(new Worker(socket, this))
      } catch {
        case e: Exception =>
          println("CHAT SERVER: Shutting down")
          running = false
          pool.shutdown()
      }
    }

    if (!serverSocket.isClosed && serverSocket != null) {
      serverSocket.close()
    }

    System.exit(0)
  }

  def Setup(portNumber: String): Unit = {
    try{
      port = Integer.parseInt(portNumber)
      serverSocket = new ServerSocket(port)
      pool = Executors.newFixedThreadPool(poolSize)
    } catch {
      case e : Exception => {
        println("CHAT SERVER: ERROR - " + e.getMessage)
        System.exit(0)
      }
    }
  }


  def shutdown = {
    println("CHAT SERVER: Closing server socket")
    serverSocket.close()
  }

  def getUniqueId:Int = synchronized{
    uniqueId = uniqueId + 1
    uniqueId
  }

  def addGroup(newGroup:Group):Unit = {
    groups = newGroup :: groups
  }

  def groupExists(groupName:String):Boolean = {
    groups.nonEmpty && groups.exists(g => g.groupName == groupName)
  }

  def groupExists(groupId:Int):Boolean = {
    groups.nonEmpty && groups.exists(g => g.groupId == groupId)
  }

  def getGroup(groupName:String):Group = {
    if(groups.nonEmpty){
      for(g <- groups){
        if(g.groupName == groupName){
          return g
        }
      }
    }
    null
  }

  def getGroup(groupId:Int):Group = {
    if(groups.nonEmpty){
      for(g <- groups){
        if(g.groupId == groupId){
          return g
        }
      }
    }
    null
  }

  def clientExists(clientName:String):Boolean = {
    clients.nonEmpty && clients.exists(c => c.handle == clientName)
  }

  def addClient(newClient:Client):Unit = {
    clients = newClient :: clients
  }

  def getClient(clientName:String):Client = {
    if(clients.nonEmpty){
      for(c <- clients){
        if(c.handle == clientName){
          return c
        }
      }
    }
    null
  }

  def removeClientFromAll(client: Client): Unit = {
    val leaveMsg = client.handle + "  has left this chatroom."

    for(g <- groups.reverse){
      if(g.hasClient(client.joinId)){
        g.sendMessage(client, leaveMsg)
        g.removeClient(client.joinId)
      }
    }
  }
}

class Worker(socket: Socket, chatServer: ChatSeverTrait) extends Runnable {
  var bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream))
  var bufferOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream)))
  val ipAddress = socket.getLocalAddress.toString.drop(1)
  val port = socket.getLocalPort
  var studentId = "b486d209d797bffeeb7e1fd3b62923902e4922ddce8eb4cc4646017d1680a52c"

  def helloMsg = "IP:" + ipAddress + "\nPort:" + port + "\nStudentID:" + studentId + "\n"

  def run(): Unit = {
    println("WORKER: " + Thread.currentThread.getId + " started")

    try {
      while (!socket.isClosed) {
        if (socket.getInputStream.available() > 0) {
          var message = ""
          message = bufferIn.readLine()
          println("WORKER: " + Thread.currentThread.getId + " received message: " + message)

          handleMessage(message)
        }
      }
    } catch {
      case e:Exception =>
        println("WORKER: " + Thread.currentThread.getId + " EXCEPTION " + e.getMessage)
    }
  }

  def handleMessage(message: String): Unit = {
    if (isHELO(message)) {
      handleHELO(message)
    } else if(isJoin(message)) {
      handleJoin(message)
    } else if(isLeave(message)) {
      handleLeave(message)
    } else if(isChat(message)) {
      handleChat(message)
    } else if(isDisconnect(message)) {
      handleDisconnect(message)
    } else if (isKillService(message)) {
      killService()
    } else {
      println("WORKER: " + Thread.currentThread.getId + " unknown message")
    }
  }

  def isHELO(message: String): Boolean = {
    message.startsWith("HELO")
  }

  def handleHELO(message: String): Unit = {
    bufferOut.println(message + "\n" + helloMsg)
    bufferOut.flush()
  }

  def isKillService(message: String): Boolean = {
    message.equals("KILL_SERVICE")
  }

  def killService(): Unit = {
    chatServer.shutdown
    socket.close()
  }

  def isJoin(message: String): Boolean = {
    message.startsWith("JOIN_CHATROOM")
  }

  def handleJoin(firstLine:String):Unit = {
    println("WORKER: " + Thread.currentThread.getId + " JOIN_CHATROOM")
    var groupName = firstLine.dropWhile(_ != ':').drop(1)

    var message = bufferIn.readLine()
    var clientIp = message.dropWhile(_ != ':').drop(1)

    message = bufferIn.readLine()
    var joinPort = message.dropWhile(_ != ':').drop(1)

    message = bufferIn.readLine()
    var clientName = message.dropWhile(_ != ':').drop(1)

    var groupId = 0
    var joinId = 0

    if(!chatServer.groupExists(groupName)){
      groupId = chatServer.getUniqueId
      chatServer.addGroup(new Group(groupName, groupId))
    } else {
      groupId = chatServer.getGroup(groupName).groupId
    }

    if(!chatServer.clientExists(clientName)){
      joinId = chatServer.getUniqueId
      chatServer.addClient(new Client(clientName, joinId, socket))
    } else {
      chatServer.getClient(clientName).socket = socket
      joinId = chatServer.getClient(clientName).joinId
    }

    var group = chatServer.getGroup(groupName)
    var client = chatServer.getClient(clientName)

    group.addClient(client)

    // Inform new client
    val joinMsg = ("JOINED_CHATROOM:" + groupName
                  + "\nSERVER_IP:" + ipAddress
                  + "\nPORT:" + port
                  + "\nROOM_REF:" + groupId
                  + "\nJOIN_ID:" + joinId)

    bufferOut.println(joinMsg)
    bufferOut.flush()

    println("WORKER: " + Thread.currentThread.getId + " sent "+ joinMsg + " to " + client.handle)

    // Inform Rest of group
    var msgToGroup = client.handle + " has joined this chatroom."
    group.sendMessage(client, msgToGroup)

    println("WORKER: " + Thread.currentThread.getId + " " + msgToGroup + " " + group.groupName)
  }

  def isLeave(message: String): Boolean = {
    message.startsWith("LEAVE_CHATROOM")
  }

  def handleLeave(firstLine:String):Unit = {
    var groupId = firstLine.dropWhile(_ != ':').drop(2).toInt

    var message = bufferIn.readLine
    var joinId = message.dropWhile(_ != ':').drop(2).toInt

    message = bufferIn.readLine
    var clientName = message.dropWhile(_ != ':').drop(2)

    var group = chatServer.getGroup(groupId)
    var client = chatServer.getClient(clientName)
    if(group != null){
      val response = "LEFT_CHATROOM: " + groupId + "\nJOIN_ID:" + joinId
      bufferOut.println(response)
      bufferOut.flush()

      var msgToGroup = clientName + " has left this chatroom."
      group.sendMessage(client, msgToGroup)

      group.removeClient(joinId)

      println("WORKER: " + Thread.currentThread.getId + " " + msgToGroup + " " + groupId);
    } else {
      println("WORKER: " + Thread.currentThread.getId + " leave group, not found " + groupId)
    }
  }

  def isChat(message: String): Boolean = {
    message.startsWith("CHAT")
  }

  def handleChat(firstLine:String):Unit = {
    var groupId = firstLine.dropWhile(_ != ':').drop(2).toInt

    var message = bufferIn.readLine
    var joinId = message.dropWhile(_ != ':').drop(2).toInt

    message = bufferIn.readLine
    var clientName = message.dropWhile(_ != ':').drop(2)

    message = bufferIn.readLine
    var sendMessage = message.dropWhile(_ != ':').drop(2)

    var group = chatServer.getGroup(groupId)
    var client = chatServer.getClient(clientName)

    group.sendMessage(client, sendMessage)

    println("WORKER: " + Thread.currentThread.getId + " sent message " + sendMessage + " to " + group )
  }

  def isDisconnect(message: String): Boolean = {
    message.startsWith("DISCONNECT")
  }

  def handleDisconnect(firstLine:String):Unit = {
    // Skip first 2 lines
    var message = bufferIn.readLine
    message  = bufferIn.readLine

    var clientName = message.dropWhile(_ != ':').drop(1)

    var client = chatServer.getClient(clientName)

    println("WORKER: " + Thread.currentThread.getId + " removing " + clientName + " from all")
    chatServer.removeClientFromAll(client)
    bufferOut.close()
    bufferIn.close()
    client.socket.close()
  }
}