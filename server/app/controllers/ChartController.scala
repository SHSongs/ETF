package controllers

import javax.inject._
import play.api.mvc._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model._

import scala.io.Source
import java.net.URL


import play.api.libs.json._
import play.api.libs.json.Json


case class StockData(date: String, open: String, high: String, low: String, close: String, adjClose: String, volume: String)

case class HistoricalData(name: String, start: String, end: String, data: List[StockData])



@Singleton
class ChartController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {


  def chart(name: String, period1: String, period2: String) = Action {
    val financeURL = s"https://finance.yahoo.com/quote/$name/history?period1=$period1&period2=$period2&interval=1d&filter=history&frequency=1d&includeAdjustedClose=true"

    val requestProperties = Map(
      "User-Agent" -> "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)"
    )

    val s = requestServer(financeURL, requestProperties)
    val info = getInformation(s)

    val historicalData = HistoricalData(name, period1, period2, info)


    for (i <- info) {
      println(i)
    }

    val data = historicalData.data.map { w =>
      Json.obj("Date" -> w.date,
        "Open" -> w.open,
        "High" -> w.high,
        "Low" -> w.low,
        "Close" -> w.close,
        "Adj Close" -> w.adjClose,
        "Volume" -> w.volume)
    }
    val json = Json.obj(
      "chart" -> Json.obj(
        "name" -> historicalData.name,
        "start" -> historicalData.start,
        "end" -> historicalData.end,
        "history" -> data
      )
    )


    Ok(json)
  }


  def getInformation(s: String): List[StockData] = {
    val browser = JsoupBrowser()
    val doc = browser.parseString(s)

    val items = doc >> "tbody" >> "tr" >> pElementList
    val x = for (i <- items) yield {
      val text = i >> "td" >> texts("span")
      val lst = text.toList
      if (lst.length > 2){
        StockData(lst(0), lst(1), lst(2), lst(3), lst(4), lst(5), lst(6))
      }
      else {
        StockData(lst(0), lst(1), lst(1), lst(1), lst(1), lst(1), lst(1))
      }
    }

    x
  }

  def requestServer(URL: String, requestProperties: Map[String, String]): String = {

    val connection = new URL(URL).openConnection
    requestProperties.foreach({
      case (name, value) => connection.setRequestProperty(name, value)
    })

    val s = Source.fromInputStream(connection.getInputStream).getLines.mkString("\n")

    s
  }
}
