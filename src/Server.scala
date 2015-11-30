import java.io.PrintStream
import java.net.{Socket, ServerSocket}
import java.util.concurrent.{Executors, ExecutorService}

import scala.io.BufferedSource

/**
 * Created by Caleb Prior on 12-Oct-15.
 */
object Server{
  var poolSize = 8
  var running = true
  var serverSocket : ServerSocket = null

  def main (args: Array[String]){
    val port = Integer.parseInt(args(0))
    serverSocket = new ServerSocket(port)
    val pool: ExecutorService = Executors.newFixedThreadPool(poolSize)

    println("Server starting on port: " + port + " with thread pool size of: " + poolSize)
    try{
      while(running){
        val socket = serverSocket.accept()
        println("Server: Connection Received")
        pool.execute(new SocketHandler(socket, shutdown, port))
      }
    } catch {
      case e: Exception =>
        running = false;
        pool.shutdown()
    } finally {
      if(serverSocket != null){
        serverSocket.close()
      }
    }
    System.exit(0)
  }

  def shutdown(): Unit ={
    serverSocket.close()
  }
}

class SocketHandler(socket: Socket, shutdown: () => Unit, port: Int) extends Runnable{
  var studentId = "b486d209d797bffeeb7e1fd3b62923902e4922ddce8eb4cc4646017d1680a52c"
  def helloMsg = "IP:"+ socket.getLocalAddress.toString.substring(1) + "\nPort:" + port + "\nStudentID:" + studentId + "\n"
  var out : PrintStream = null

  override def run(){
    try{
      val in = new BufferedSource(socket.getInputStream).getLines()
      out = new PrintStream(socket.getOutputStream)

      val msg = in.next()
      println(Thread.currentThread.getName + " Received:" + msg)

      handleMessage(msg)

      socket.close()
    } catch {
      case e: Exception =>
        println(Thread.currentThread.getName + " threw Exception:\n" + e)
    } finally {
      if(socket != null){
        socket.close()
      }
    }
  }

  def handleMessage(message: String): Unit ={
    if(isKillService(message)){
      killService()
    }else if (isHELO(message)){
      handleHELO(message)
    }
  }

  def isKillService(message: String): Boolean ={
    message.equals("KILL_SERVICE")
  }

  def killService(): Unit ={
    shutdown()
  }

  def isHELO(message: String): Boolean ={
    message.startsWith("HELO ")
  }

  def handleHELO(message: String): Unit ={
    out.println(message+"\n"+helloMsg)
    out.flush()
  }
}
