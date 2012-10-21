package controllers.controllersSupport

import play.api.libs.json.{JsValue, Json}
import play.api.cache.Cache
import play.api.Play.current
import play.api.libs.oauth.{RequestToken, OAuthCalculator, ConsumerKey}
import play.api.libs.ws.WS
import play.libs.{WS => WWSS}
import java.net.{URLEncoder, URLDecoder}

object TwitterSupport {

  initial

  /*
  * 初期化処理
  * ログインとかする予定
  */
  def initial {}


  /*
  * 指定された地域コードのトレンドを返す
  * twitter側の情報は5分毎に更新
  * @param id 地域コード
  * @return twitterトレンド(JsValue)
  */
  def getHomeTimeline(oauthCalculator: OAuthCalculator): JsValue = {
    //println("UserTimeline []")
    val url = "https://api.twitter.com/1.1/statuses/home_timeline.json"

    val resultPromise = WS.url(url)
      //.withQueryString(("q", query))
      .sign(oauthCalculator)
      .get()
    if (resultPromise.await.get.status != 200) throw new NoSuchFieldException("Twitterへのアクセスでエラーが発生しました : " + resultPromise.await.get.status.toString)
    val responseBody = resultPromise.await.get.body

    //println(responseBody)
    val responseJson = Json.parse(responseBody)
    responseJson
  }

  def postStatusUpdate(oauthCalculator: OAuthCalculator, message: String): JsValue = {
    //println("StatusUpdate [" + message + "]")
    val url = "https://api.twitter.com/1.1/statuses/update.json?status="+URLEncoder.encode(message, "UTF-8")

    val resultPromise = WS.url(url)
      .sign(oauthCalculator)
      .post("")
      //.post("status="+"Post!")
      //.post(Map("status" -> Seq(message)))

    if (resultPromise.await.get.status != 200) throw new NoSuchFieldException("Twitterへのアクセスでエラーが発生しました : " + resultPromise.await.get.body + " : " + resultPromise.await.get.status.toString)
    val responseBody = resultPromise.await.get.body

    //println(responseBody)
    val responseJson = Json.parse(responseBody)
    responseJson

  }

}
