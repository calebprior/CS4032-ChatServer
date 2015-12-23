import java.io.PrintWriter
import java.net.{InetAddress, Socket}

import scala.io.BufferedSource

/**
 * Created by Caleb Prior on 12-Oct-15.
 */
object EchoClient {
  def main(args: Array[String]) {
    val s = new Socket(InetAddress.getByName("178.62.121.108"), 443)
    lazy val in = new BufferedSource(s.getInputStream).getLines()
    val out = new PrintWriter(s.getOutputStream)

    val message = io.StdIn.readLine("Enter Message> ")
    out.println(message + "\n")
    out.flush()

    println("Sent: " + message)
    while(in.hasNext)
      println("Received: " + in.next)
    s.close()
  }
}