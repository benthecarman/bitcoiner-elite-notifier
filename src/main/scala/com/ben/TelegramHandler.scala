package com.ben

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import com.ben.config.BENAppConfig
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import lnrpc.{Failure => _, _}
import org.bitcoins.core.currency._
import org.bitcoins.core.protocol.ln.LnInvoice
import org.bitcoins.lnd.rpc.LndRpcClient
import sttp.capabilities.WebSockets
import sttp.client3.SttpBackend
import sttp.client3.okhttp.OkHttpFutureBackend

import java.net.URLEncoder
import java.text.NumberFormat
import scala.concurrent.Future
import scala.util._
import scala.util.matching.Regex

class TelegramHandler(lnd: LndRpcClient)(implicit
    config: BENAppConfig,
    system: ActorSystem)
    extends TelegramBot
    with Polling
    with Commands[Future] {

  val numericRegex: Regex = "^[0-9]*\\.[0-9]{2}$ or ^[0-9]*\\.[0-9][0-9]$".r
  val numberFormatter: NumberFormat = java.text.NumberFormat.getIntegerInstance

  private val myTelegramId = config.telegramId
  private val telegramCreds = config.telegramCreds

  private val http = Http(system)

  implicit val backend: SttpBackend[Future, WebSockets] =
    OkHttpFutureBackend()

  override val client: RequestHandler[Future] =
    new FutureSttpClient(telegramCreds)

  onCommand("version") { _ =>
    sendTelegramMessage(getClass.getPackage.getImplementationVersion)
  }

  onCommand("invoice") { msg =>
    val invoiceT = Try {
      msg.text match {
        case Some(text) =>
          val params = text.trim.split(" ", 3).tail
          val satsOpt = params.headOption.map(_.toLong)
          val memoOpt = params.lastOption

          (satsOpt, memoOpt) match {
            case (None, None)             => Invoice()
            case (Some(sats), None)       => Invoice(value = sats)
            case (None, Some(memo))       => Invoice(memo = memo)
            case (Some(sats), Some(memo)) => Invoice(memo = memo, value = sats)
          }
        case None => Invoice()
      }
    }

    invoiceT match {
      case Failure(exception) =>
        sendTelegramMessage(s"Error creating invoice ${exception.getMessage}")
      case Success(invoice) =>
        lnd
          .addInvoice(invoice)
          .map(_.invoice.toString)
          .recover { case err: Throwable =>
            s"Failed to create invoice ${err.getMessage}"
          }
          .flatMap(sendTelegramMessage)
    }
  }

  onCommand("pay") { msg =>
    msg.text match {
      case Some(text) =>
        val invoiceStr = text.trim.split(" ", 2).last
        val invoice = LnInvoice.fromString(invoiceStr)
        if (invoice.amount.get.toSatoshis <= config.maxPayment) {
          lnd.sendPayment(invoice).flatMap { resp =>
            resp.paymentRoute match {
              case Some(route) =>
                val amt = printAmount(Satoshis(route.totalAmt))
                val fees = printAmount(Satoshis(route.totalFees))

                sendTelegramMessage(
                  s"$amt invoice paid, fees $fees, hops ${route.hops.size}")
              case None =>
                sendTelegramMessage(
                  s"Failed to pay invoice: ${resp.paymentError}")
            }
          }
        } else {
          sendTelegramMessage(
            s"Invoice amount is above configured max payment ${config.maxPayment}")
        }
      case None => sendTelegramMessage("Error, no invoice given")
    }
  }

  def sendTelegramMessage(message: String): Future[Unit] = {
    val url = s"https://api.telegram.org/bot$telegramCreds/sendMessage" +
      s"?chat_id=${URLEncoder.encode(myTelegramId, "UTF-8")}" +
      s"&text=${URLEncoder.encode(message.trim, "UTF-8")}"

    http.singleRequest(Get(url)).map(_ => ())
  }

  def printSize(size: Long): String = {
    if (size < 1000) {
      s"${numberFormatter.format(size)} vbytes"
    } else if (size < 1000000) {
      s"${numberFormatter.format(size / 1000.0)} vKB"
    } else if (size < 1000000000) {
      s"${numberFormatter.format(size / 1000000.0)} vMB"
    } else {
      s"${numberFormatter.format(size / 1000000000.0)} vGB"
    }
  }

  def printAmount(amount: CurrencyUnit): String = {
    numberFormatter.format(amount.satoshis.toLong) + " sats"
  }
}
