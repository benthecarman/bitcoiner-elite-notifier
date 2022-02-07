package com.ben

import akka.Done
import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.ben.config.BENAppConfig
import grizzled.slf4j.Logging
import lnrpc.Invoice
import lnrpc.Invoice.InvoiceState._
import org.bitcoins.core.currency.Satoshis
import org.bitcoins.lnd.rpc.LndRpcClient

import scala.concurrent._

case class LndSubscriber(lnd: LndRpcClient)(implicit
    system: ActorSystem,
    config: BENAppConfig)
    extends Logging {
  implicit val ec: ExecutionContext = system.dispatcher

  val telegram = new TelegramHandler(lnd)

  def startInvoiceSubscription(): Future[Done] = {
    val parallelism = Runtime.getRuntime.availableProcessors()

    lnd
      .subscribeInvoices()
      .mapAsync(parallelism) { invoice =>
        invoice.state match {
          case OPEN | ACCEPTED | CANCELED | Unrecognized(_) =>
            Future.unit // do nothing
          case SETTLED => handleSettledInvoice(invoice)
        }
      }
      .runWith(Sink.ignore)
  }

  private def handleSettledInvoice(inv: Invoice): Future[Unit] = {
    val amt = telegram.printAmount(Satoshis(inv.amtPaidSat))
    val msg = s"\uD83D\uDCB5 Received $amt - ${inv.memo}"
    telegram.sendTelegramMessage(msg)
  }
}
