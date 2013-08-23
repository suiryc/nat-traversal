package nat.traversal.util

import akka.actor.{Actor, ActorRef}
import akka.io.Udp
import akka.util.ByteString
import com.typesafe.scalalogging.slf4j.Logging
import java.net.{
  DatagramPacket,
  InetSocketAddress,
  MulticastSocket
}
import scala.concurrent.ops._

/* Mimic original Udp class hierarchy */
object UdpMc {

  sealed trait Message

  trait Event extends Message

  trait Command extends Message

  case class Bind(handler: ActorRef, socket: MulticastSocket) extends Command

  case object Bound extends Event

}

class UdpMcService
  extends Actor
  with Logging
{

  import UdpMc._

  def receive = unbound()

  def unbound(): Receive = {
    case Bind(handler, socket) =>
      listen(handler, socket)

      sender ! Bound
      context.become(ready(handler, socket))
  }

  def ready(handler: ActorRef, socket: MulticastSocket): Receive = {
    case Udp.Received(data, remote) =>
      handler ! Udp.Received(data, remote)

    case Udp.Send(data, target, ack) =>
      val bytes = data.toArray
      socket.send(new DatagramPacket(bytes, bytes.length, target))

    case Udp.Unbind =>
      socket.close()
      /* listener (below) will send the Unbound event */
  }

  private def listen(handler: ActorRef, socket: MulticastSocket) {
    /* XXX - deprecated, replace by explicit new Thread(new Runnable()) ? */
    spawn {
      val dgramBuffer = new Array[Byte](1 * 1024)
      val dgram = new DatagramPacket(dgramBuffer, dgramBuffer.length)

      @scala.annotation.tailrec
      def receive(): Unit = {
        val (received, closed) = try {
          socket.receive(dgram)
          (true, false)
        }
        catch {
          case e: Throwable =>
            val r = socket.isClosed
            if (!r) {
              logger.error("Exception caught while receiving next datagram", e)
            }
            (false, r)
        }

        if (closed) {
          handler ! Udp.Unbound
          context.become(unbound())
        }
        else {
          if (received) {
            handler ! Udp.Received(ByteString.fromArray(dgramBuffer, 0, dgram.getLength),
              new InetSocketAddress(dgram.getAddress, dgram.getPort))
          }
          dgram.setLength(dgramBuffer.length)
          receive()
        }
      }

      receive()
    }
  }

}
