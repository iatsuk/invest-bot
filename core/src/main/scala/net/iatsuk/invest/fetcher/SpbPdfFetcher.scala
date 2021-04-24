/*
  Copyright 2021 Andrei Iatsuk

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package net.iatsuk.invest.fetcher

import java.io.{BufferedInputStream, ByteArrayOutputStream, File}
import java.net.URL
import java.nio.file.Files

import io.circe.generic.auto._
import io.circe.syntax._
import io.github.jonathanlink.PDFLayoutTextStripper
import net.iatsuk.invest.Context
import net.iatsuk.invest.domain.DayStocks
import net.iatsuk.invest.fetcher.SpbPdfFetcher.STOCKS_LIST
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import org.apache.pdfbox.io.{RandomAccessBuffer, RandomAccessRead}
import org.apache.pdfbox.pdfparser.PDFParser
import shapeless.HList._
import shapeless._
import shapeless.syntax.std.traversable._

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.util.Try

@Fetcher(name = "spb-pdf")
class SpbPdfFetcher extends FetcherOps {

  private val host = "https://archives.spbexchange.ru/"
  private val indexPage = f"$host/reports/results/"
  private val pdfFilter = "eve.pdf"
  private val dayFilter = "day.json"

  private[fetcher] val browser = JsoupBrowser()
  private var stocksListing: Map[String, Array[String]] = _

  private var stopped = false

  override def sync()(implicit ctx: Context): Unit = {
    loadStocksListing()
    proceedPdf()
    proceedJson()
  }

  /**
   * @see https://spbexchange.ru/ru/listing/securities/list/ -> "Скачать CSV"
   */
  private[fetcher] def loadStocksListing()(implicit ctx: Context): Unit = {
    if (stocksListing == null) {
      val defaultStockList = getClass.getClassLoader.getResource("ListingSecurityList.csv").getFile
      val csv = new File(ctx.conf.getProperty(STOCKS_LIST, defaultStockList))
      stocksListing = Files.readAllLines(csv.toPath).asScala
        .tail
        .map(line => line.split(";"))
        .map(line => Try(line(7) -> Array(line(2), line(5), line(6))).recover {
          err =>
            println(f"ERROR $err on ${line.mkString("Array(", ", ", ")")}")
            "" -> Array("")
        }.get).toMap
    }
  }

  private[fetcher] def proceedPdf()(implicit ctx: Context): Unit = {
    val index = browser.get(indexPage)

    val urls = extractUrls(index, pdfFilter).map(part => f"$host/$part").map(new URL(_))

    val exists = ctx.storage.keys.map(new String(_))
    val fresh = urls.filterNot(url => exists.contains(extractFileName(url)))
    println(f"New data are: ${fresh.mkString("[", ", ", "]")}")

    for (url <- fresh if !stopped) {
      val name = f"${extractFileName(url)}".getBytes()
      val data = fetch(url)
      ctx.storage.put(name, data)
    }
  }

  private[fetcher] def extractUrls(doc: browser.DocumentType, filter: String): Seq[String] =
    doc >> elementList("a") >> attr("href") filter (_.contains(filter))

  private[fetcher] def extractFileName(url: URL): String = {
    val raw = url.toString
    val index = raw.lastIndexOf('/') + 1
    raw.substring(index)
  }

  /**
   * @see https://www.baeldung.com/java-download-file
   */
  private[fetcher] def fetch(url: URL): Array[Byte] = {
    val in = new BufferedInputStream(url.openStream());
    val out = new ByteArrayOutputStream()

    val bufferSize = 4 * 1024
    val dataBuffer = new Array[Byte](bufferSize)
    var bytesRead = in.read(dataBuffer, 0, bufferSize)
    while (bytesRead != -1) {
      out.write(dataBuffer, 0, bytesRead)
      bytesRead = in.read(dataBuffer, 0, bufferSize)
    }

    out.toByteArray
  }

  private[fetcher] def proceedJson()(implicit ctx: Context): Unit = {
    val exists = ctx.storage.keys.map(new String(_))

    val pdfs = exists.filter(_.endsWith(pdfFilter))
    val parsed = exists.filter(_.endsWith(dayFilter))
    val dateLen = "yyyy-MM-dd".length
    val days2parse = pdfs.map(_.substring(0, dateLen)) diff parsed.map(_.substring(0, dateLen))

    for (day <- days2parse) {
      val pdfKey = f"$day-$pdfFilter".getBytes
      val jsonKey = f"$day-$dayFilter".getBytes
      ctx.storage.get(pdfKey).toTry
        .map(pdfData => parse(pdfData, stocksListing).map(_.copy(date = day)))
        .map(dayData => dayData.asJson.noSpaces)
        .map(json => ctx.storage.put(jsonKey, json.getBytes))
        .fold(err => println(f"ERROR ${err.getMessage}"), x => x.getOrElse())
    }
  }

  private[fetcher] def parse(pdf: Array[Byte], stocksListing: Map[String, Array[String]]): Seq[DayStocks] =
    for (stock <- parseStocksPdf(new RandomAccessBuffer(pdf)))
      yield if (stocksListing.contains(stock.isin)) {
        val meta = stocksListing(stock.isin)
        val (title, category, code) = (meta(0), meta(1), meta(2))
        stock.copy(code = code, title = title, category = category)
      } else {
        println(f"ERROR Meta for ${stock.isin} not found")
        stock.copy()
      }

  private[fetcher] def parseStocksPdf(source: RandomAccessRead): Seq[DayStocks] = {
    // parse
    val parser = new PDFParser(source)
    parser.parse()
    val stripper = new PDFLayoutTextStripper()
    stripper.setAddMoreFormatting(true)
    val doc = parser.getPDDocument
    val text = stripper.getText(doc)

    // tokenize
    val parsed = text.split("\n").map(DayStocksParser.parse).filter(_.isDefined).map(_.get)
    parsed
  }

  override def stop(): Unit = {
    stopped = true
  }
}

private object DayStocksParser {
  def parse(text: String): Option[DayStocks] = {
    object Step extends Enumeration {
      type Step = Value
      val CODE, ISIN, CUR, PRICE, END = Value
    }
    import Step._

    val result = mutable.Buffer[Any]()
    val buf = mutable.Buffer[Char]()
    var step = CODE
    for (c <- text.toCharArray) {
      step match {
        case CODE =>
          if (c.isUpper || c.isDigit || c == '.' || c == '-' || c == '@') buf.append(c)
          else if (c.isWhitespace && buf.nonEmpty) {
            result.addOne("")
            result.addOne(buf.mkString)
            result.addOne("")
            result.addOne("")
            buf.clear()
            step = ISIN
          } else buf.clear()
        case ISIN =>
          if (c.isUpper) buf.append(c)
          else if (buf.length >= 2 && c.isDigit) buf.append(c)
          else if (buf.length == 12 && c.isWhitespace) {
            result.addOne(buf.mkString)
            buf.clear()
            step = CUR
          } else buf.clear()
        case CUR =>
          if (c.isUpper) buf.append(c)
          else if (buf.length == 3 && c.isWhitespace) {
            result.addOne(buf.mkString)
            buf.clear()
            step = PRICE
          } else buf.clear()
        case PRICE =>
          if (result.length == 9) step = END
          else if (c.isDigit || c == ',') buf.append(c)
          else if (buf.nonEmpty && c.isWhitespace) {
            val value = buf.mkString.replace(',', '.').toFloat
            result.addOne(value)
            buf.clear()
          } else buf.clear()
        case END => // Nothing to do
      }
    }

    result.toHList[String :: String :: String :: String :: String :: String :: Float :: Float :: Float :: HNil]
      .map(h => DayStocks.tupled(h.tupled))
  }
}

object SpbPdfFetcher {
  val STOCKS_LIST = "fetch.stocksListing"
}