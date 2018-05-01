package api.tasks

import org.json4s.JsonAST.JValue

trait TaskSchema {

  def toJson: JValue

  def extract(json: JValue): Option[AnyRef]


}

