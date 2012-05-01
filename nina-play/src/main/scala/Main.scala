package nina.impl.play

import nina._
import nina.impl._

import java.util.Date
import java.sql.PreparedStatement

trait NinaPlayImplPackage {
	def ninaSetterToStatement[T]: anorm.ToStatement[T] = new anorm.ToStatement[T] {
		def set(s: PreparedStatement, index: Int, aValue: T) { NinaSetter.set[T](s, index, aValue) }
	}
}