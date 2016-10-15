package controllers

import play.api.mvc._

class RootController extends Controller {
  def index = Action {
    Ok("FOO")
  }
}
