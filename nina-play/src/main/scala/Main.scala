package nina.impl.play

import nina._

import java.util.Date
import java.sql.PreparedStatement

trait NinaPlayImplPackage {
	implicit def toStatement2ninaSetter[T](stmt: anorm.ToStatement[T]): NinaSetter[T] = new NinaSetter[T] {
		def set(s: PreparedStatement, index: Int, value: T) { stmt.set(s, index, value) }
	}
	implicit def ninaSetter2toStatement[T](setter: NinaSetter[T]): anorm.ToStatement[T] = new anorm.ToStatement[T] {
		def set(s: PreparedStatement, index: Int, aValue: T) { setter.set(s, index, aValue) }
	}

	implicit def anySetter[T]: NinaSetter[T] = anorm.ToStatement.anyParameter[T]
	implicit val dateSetter: NinaSetter[java.util.Date] = anorm.ToStatement.dateToStatement
	implicit def optionSetter[T](implicit ts: NinaSetter[T]): NinaSetter[Option[T]] = anorm.ToStatement.optionToStatement[T](ts)
}