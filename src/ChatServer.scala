import java.io._
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

trait ChatSeverTrait {
  def shutdown()
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
      } finally {
        if(!serverSocket.isClosed  && serverSocket!= null){
          serverSocket.close()
        }
      }
      System.exit(0)
    }
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


  def shutdown() = {
    println("CHAT SERVER: Closing server socket")
    serverSocket.close()
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
        println("WORKER: " + Thread.currentThread + " EXCEPTION " + e.getMessage)
    }
  }

  def handleMessage(message: String): Unit = {
    if (isHELO(message)) {
      handleHELO(message)
    } else if(isJoin(message)) {
      handleJoin(message)
    } else if(isLeave(message)) {

    } else if(isChat(message)) {

    } else if(isDisconnect(message)) {

    } else if (isKillService(message)) {
      killService()
    } else {
      println("WORKER: " + Thread.currentThread + " unknown message")
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
    chatServer.shutdown()
  }

  def isJoin(message: String): Boolean = {
    message.startsWith("JOIN_CHATROOM")
  }

  def handleJoin(message:String):Unit = {

  }

  def isLeave(message: String): Boolean = {
    message.startsWith("LEAVE_CHATROOM")
  }

  def isChat(message: String): Boolean = {
    message.startsWith("CHAT")
  }

  def isDisconnect(message: String): Boolean = {
    message.startsWith("DISCONNECT")
  }
}