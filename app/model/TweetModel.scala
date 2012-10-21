package models

import java.net.URL
import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString

/*
 * twitterのツイートを格納するcase class
 */
case class TweetModel(
                       text: String,
                       created_at: String,
                       screen_name: String,
                       name: String,
                       profile_image_url:URL
                       )

object TweetModel {

  implicit object TweetModelFormat extends Format[TweetModel] {
    /*
     * JsonからModelに変換
     */
    def reads(json: JsValue): TweetModel = TweetModel(
      (json \ "text").as[String],
      (json \ "created_at").as[String],
      (json \ "user" \ "screen_name").as[String],
      (json \ "user" \ "name").as[String],
      new URL((json \ "user" \ "profile_image_url").as[String])
    )

    /*
     * ModelからJsonに変換
     */
    def writes(t: TweetModel): JsValue = JsObject(
      List(
        "text" -> JsString(t.text),
        "created_at" -> JsString(t.created_at),
        "screen_name" -> JsString(t.screen_name),
        "name" -> JsString(t.name),
        "profile_image_url" -> JsString(t.profile_image_url.toString)
      )
    )

  }

}