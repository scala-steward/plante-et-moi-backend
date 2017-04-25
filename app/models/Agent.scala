package models

import javax.inject.Inject

import services.SettingService
import utils.Hash
import play.api.libs.json._
import play.api.libs.json.Reads._

case class Agent(id: String,
                 city: String,
                 name: String,
                 qualite: String,
                 email: String,
                 key: String,
                 admin: Boolean,
                 instructor: Boolean,
                 canReview: Boolean,
                 finalReview: Boolean)

@javax.inject.Singleton
class AgentService @Inject()(configuration: play.api.Configuration, settingService: SettingService) {
  private lazy val cryptoSecret = configuration.underlying.getString("play.crypto.secret")

  private implicit def resultReads(city: String): Reads[Agent] = {
    JsPath.json.update(
      (JsPath \ 'key).json.copyFrom((JsPath \ 'email).json.pick[JsString].map{ jsString => JsString(Hash.sha256(s"${jsString.value}$city$cryptoSecret")) })
    ) andThen JsPath.json.update(
      (JsPath \ 'id).json.copyFrom((JsPath \ 'email).json.pick[JsString].map{ jsString => JsString(Hash.md5(s"${jsString.value}$city$cryptoSecret")) })
    ) andThen JsPath.json.update(
      (JsPath \ 'city).json.put(JsString(city))
    ) andThen Json.reads[Agent]
  }

  def defaultAgents(city: String) = List(
    Agent(
      "celia",
      city,
      "Celia Henry",
      "Administratrice Plante Et Moi",
      ("celia.henry"+"@beta.gouv.fr"),
      Hash.sha256(s"celia$city$cryptoSecret"),
      true,
      true,
      false,
      false
    ),
    Agent(
      "julien",
      city,
      "Julien Dauphant",
      "Administrateur Plante Et Moi",
      ("julien.dauphant"+"@beta.gouv.fr"),
      Hash.sha256(s"julien$city$cryptoSecret"),
      true,
      true,
      false,
      false
    )
  )

  def all(city: String) = {
    implicit val agentReads = resultReads(city)
    settingService.findByKey(city)("AGENTS").flatMap(_._2.validate[List[Agent]].asOpt).getOrElse(List()) ++ defaultAgents(city)
  }

  def byId(city: String)(id: String) = all(city).find(_.id == id)

  def byKey(city: String)(key: String) = all(city).find(_.key == key)
}