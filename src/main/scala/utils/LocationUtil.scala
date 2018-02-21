package utils

import play.api.libs.json.{JsValue, Json}

import scalaj.http.{Http, HttpResponse}

object LocationUtil {

  def getLocationCoordinates(location: String) = {
    val response : HttpResponse[String] = Http("https://maps.googleapis.com/maps/api/geocode/json")
      .params(Seq("address" -> "905 S Dorsey Ln, Tempe, Arizona", "key" -> "AIzaSyBhMCtC_N5y2Xq8meEAOwI9ft-8MNySwk8")).asString

    val json: JsValue = Json.parse(response.body)

    ((json\"results"\0\"geometry"\"location"\"lat").get, (json\"results"\0\"geometry"\"location"\"lng").get)
  }

}
