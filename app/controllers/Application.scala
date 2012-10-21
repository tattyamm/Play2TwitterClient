package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import libs.oauth.ConsumerKey
import libs.oauth.OAuth
import libs.oauth.OAuthCalculator
import libs.oauth.RequestToken
import libs.oauth.ServiceInfo
import mvc._
import controllers.controllersSupport.TwitterSupport
import models.TweetModel
import scala.Left
import scala.Right


object Application extends Controller {

  //twitterでアプリケーションを登録した時に取得する値
  //動かす時はここに入力してください。
  val consumerKey = ""
  val consumerSecret = ""
  //callbackURLを設定
  val retrieveRequestToken = ""

  /**
   * ログイン、ログアウト判定を行い、適切に振り分ける
   * @return
   */
  def index = Action {
    request => {
      //セッションの中身の取得
      val token = getKeyFromSession(request)
      val accessToken = token.getOrElse('accessToken, "")
      val accessTokenSecret = token.getOrElse('accessTokenSecret, "")

      //ログイン判定
      // TODO こんな方法はダメなのでは。
      if (!(accessToken == "" && accessTokenSecret == "")) {
        Redirect(routes.Application.timeline)
      } else {
        Ok(views.html.index(""))
      }

    }
  }


  /*
  * つぶやきを表示する
  */
  def timeline = Action {
    request => {
      //セッションの中身の取得
      val token = getKeyFromSession(request)
      val accessToken = token.getOrElse('accessToken, "")
      val accessTokenSecret = token.getOrElse('accessTokenSecret, "")

      //ログイン判定
      // TODO こんな方法はダメなのでは。
      if (!(accessToken == "" && accessTokenSecret == "")) {
      } else {
        Redirect(routes.Application.index)
      }

      //認証情報の作成
      val oauthCalculator = OAuthCalculator(ConsumerKey(consumerKey, consumerSecret), RequestToken(accessToken, accessTokenSecret))

      val userTimeline = TwitterSupport.getHomeTimeline(oauthCalculator)
      val userTimelineList = userTimeline.toString.drop(1).dropRight(1).replace("},{\"created_at", "},,,,,,,,,,,,,,,,,,,,{\"created_at").split(",,,,,,,,,,,,,,,,,,,,") //TODO 強引すぎる変換(ラベルの無いカンマ区切りのjsonはどうすれば良いのか。
      //debug
      //userTimelineList.foreach { tweet => println(tweet + "====") }

      val userTimelineModelList = userTimelineList.map {
        play.api.libs.json.Json.parse(_).as[TweetModel]
      }

      //結果表示
      Ok(views.html.timeline(userTimelineModelList))
    }
  }

  /**
   * つぶやきを投稿する
   * @return
   */
  def post = Action {
    implicit request =>
    //パラメーター受け取り
      val form = Form(
        "message" -> nonEmptyText //複数の場合はtupleにするっぽい
      )
      val message = form.bindFromRequest.fold(
        errors => throw new IllegalArgumentException("投稿メッセージが受け取れませんでした"),
        message => {
          message
        }
      )

      //セッションの中身の取得
      val token = getKeyFromSession(request)
      val accessToken = token.getOrElse('accessToken, "")
      val accessTokenSecret = token.getOrElse('accessTokenSecret, "")

      //ログイン判定
      // TODO こんな方法はダメなのでは。
      if (!(accessToken == "" && accessTokenSecret == "")) {
      } else {
        Redirect(routes.Application.index)
      }

      //認証情報の作成
      val oauthCalculator = OAuthCalculator(ConsumerKey(consumerKey, consumerSecret), RequestToken(accessToken, accessTokenSecret))

      val userTimeline = TwitterSupport.postStatusUpdate(oauthCalculator, message) //TODO ここに投稿内容

      //結果表示
      Redirect(routes.Application.timeline)
  }

  /*
  * セッションを破棄してindexページにリダイレクト
  */
  def logout = Action {
    Redirect(routes.Application.index).withNewSession
  }


  /**
   * セッションからaccess keyとtokenを持ってくる
   * @param request
   * @return accessTokenとaccessTokenSecret
   */
  def getKeyFromSession(request: Request[AnyContent]): Map[Symbol, String] = {
    val accessToken = request.session.get("token").map {
      token =>
      //println("token : " + token)
        token
    }.getOrElse {
      //println("tokenが取得できなかった")
      ""
    }
    val accessTokenSecret = request.session.get("secret").map {
      secret =>
      //println("secret : " + secret)
        secret
    }.getOrElse {
      //println("secretが取得できなかった")
      ""
    }

    Map('accessToken -> accessToken, 'accessTokenSecret -> accessTokenSecret)
  }


  /**
   * Aboutページを表示
   * @return
   */
  def about = Action {
    Ok(views.html.about(""))
  }

  /*
  * twitter認証系のテスト
  * ここからほぼコピーしています　https://github.com/playframework-ja/Play20/wiki/ScalaOAuth
  */
  val KEY = ConsumerKey(consumerKey, consumerSecret)
  val TWITTER = OAuth(ServiceInfo(
    "https://api.twitter.com/oauth/request_token",
    "https://api.twitter.com/oauth/access_token",
    "https://api.twitter.com/oauth/authorize", KEY),
    false)

  def authenticate = Action {
    request =>
      request.queryString.get("oauth_verifier").flatMap(_.headOption).map {
        verifier =>
          val tokenPair = sessionTokenPair(request).get
          // We got the verifier; now get the access token, store it and back to index
          //println("認証されました。アクセストークンを取得し、保存し、indexに戻ります")
          TWITTER.retrieveAccessToken(tokenPair, verifier) match {
            case Right(t) => {
              // We received the authorized tokens in the OAuth object - store it before we proceed
              //println("Oauthオブジェクトからアクセストークンを受け取りました。それを保存します。")
              Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
            }
            case Left(e) => throw e
          }
      }.getOrElse(
        TWITTER.retrieveRequestToken("http://192.168.11.2:9000/auth") match { //環境によりURL変更の必要
          //コールバックURL
          case Right(t) => {
            // We received the unauthorized tokens in the OAuth object - store it before we proceed
            //println("認証されてないトークンを受け取りました。それを保存します。")
            Redirect(TWITTER.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
          }
          case Left(e) => throw e
        })
  }

  def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
    for {
      token <- request.session.get("token")
      secret <- request.session.get("secret")
    } yield {
      RequestToken(token, secret)
    }
  }


}