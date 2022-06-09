package com.acme
package domain

sealed trait Component{
  override def toString: String = getClass.getSimpleName.replace("$","")
}

object Component {
  case object Broom extends Component
  case object Mop extends Component
  case object MainUnit extends Component
}
