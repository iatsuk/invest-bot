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

import java.io.File

import net.iatsuk.invest.domain.DayStocks
import org.apache.pdfbox.io.RandomAccessFile
import org.scalatest.flatspec._
import org.scalatest.matchers._

class SpbPdfFetcherSpec extends AnyFlatSpec with should.Matchers {

  "extractLinks" should "extract all links from page" in {
    val html =
      """<html><body>
        |  <a href="/reports/results/2018-01-02-eve.pdf">2018-01-02-eve.pdf</a>
        |  <a href="/reports/results/2018-01-03-eve.pdf">2018-01-03-eve.pdf</a>
        |  <a href="/reports/results/2018-01-03-full.pdf">2018-01-03-full.pdf</a>
        |</body></html>""".stripMargin

    val fetcher = new SpbPdfFetcher
    val doc = fetcher.browser.parseString(html)

    fetcher.extractUrls(doc, "eve.pdf") should contain theSameElementsAs Seq(
      "/reports/results/2018-01-02-eve.pdf",
      "/reports/results/2018-01-03-eve.pdf"
    )
  }

  "fetch" should "copy file by url" in {
    val in = getClass.getClassLoader.getResource("2021-02-10-eve.pdf")

    val fetcher = new SpbPdfFetcher
    val result = fetcher.fetch(in)

    result.length shouldBe 364874
  }

  "parseStocksPdf" should "extract all data from pdf" in {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog")
    val source = new File(getClass.getClassLoader.getResource("2021-02-10-eve.pdf").toURI)

    val fetcher = new SpbPdfFetcher

    fetcher.parseStocksPdf(new RandomAccessFile(source, "r")) should contain theSameElementsAs Seq(
      DayStocks("", "A", "", "", "US00846U1016", "USD", 123.16f, 125.08f, 124.25f),
      DayStocks("", "AA", "", "", "US0138721065", "USD", 21.05f, 22.11f, 21.25f),
      DayStocks("", "AAL", "", "", "US02376R1023", "USD", 17.28f, 17.71f, 17.47f),
      DayStocks("", "AAN", "", "", "US00258W1080", "USD", 20.0f, 20.81f, 20.43f),
      DayStocks("", "AAON", "", "", "US0003602069", "USD", 80.01f, 81.36f, 80.07f),
      DayStocks("", "AAP", "", "", "US00751Y1064", "USD", 158.53f, 162.35f, 158.89f),
      DayStocks("", "AAPL", "", "", "US0378331005", "USD", 134.41f, 137.34f, 135.12f),
      DayStocks("", "ABBV", "", "", "US00287Y1091", "USD", 103.74f, 106.4f, 104.47f),
      DayStocks("", "ABC", "", "", "US03073E1055", "USD", 106.7f, 108.03f, 106.7f),
      DayStocks("", "ABG", "", "", "US0434361046", "USD", 158.38f, 165.0f, 160.39f),
      DayStocks("", "ABMD", "", "", "US0036541003", "USD", 321.88f, 328.0f, 322.79f),
      DayStocks("", "ABNB", "", "", "US0090661010", "USD", 200.62f, 214.47f, 207.38f),
      DayStocks("", "ABT", "", "", "US0028241000", "USD", 124.9f, 126.12f, 125.71f),
      DayStocks("", "ACAD", "", "", "US0042251084", "USD", 50.0f, 51.96f, 51.7f),
      DayStocks("", "ACH", "", "", "US0222761092", "USD", 8.7f, 8.93f, 8.72f),
      DayStocks("", "ACIA", "", "", "US00401C1080", "USD", 114.8f, 115.83f, 114.83f),
      DayStocks("", "ACIW", "", "", "US0044981019", "USD", 40.83f, 40.83f, 41.47f),
      DayStocks("", "ACM", "", "", "US00766T1007", "USD", 52.8f, 53.91f, 52.79f),
      DayStocks("", "ACMR", "", "", "US00108J1097", "USD", 104.64f, 111.23f, 105.8f),
      DayStocks("", "ACN", "", "", "IE00B4BNMY34", "USD", 256.84f, 259.0f, 257.35f),
      DayStocks("", "ADBE", "", "", "US00724F1012", "USD", 490.65f, 499.31f, 492.57f),
      DayStocks("", "ADI", "", "", "US0326541051", "USD", 151.71f, 154.33f, 152.22f),
      DayStocks("", "ZNH", "", "", "US1694091091", "USD", 31.29f, 32.5f, 31.29f),
      DayStocks("", "ZS", "", "", "US98980G1022", "USD", 216.7f, 224.0f, 218.69f),
      DayStocks("", "ZTS", "", "", "US98978V1035", "USD", 159.83f, 162.0f, 160.66f),
      DayStocks("", "ZUMZ", "", "", "US9898171015", "USD", 46.77f, 49.12f, 47.2f),
      DayStocks("", "ZUO", "", "", "US98983V1061", "USD", 16.02f, 16.88f, 16.17f),
      DayStocks("", "ZYNE", "", "", "US98986X1090", "USD", 6.01f, 7.35f, 6.84f),
      DayStocks("", "ZYXI", "", "", "US98986M1036", "USD", 19.05f, 20.46f, 19.51f)
    )
  }
}
