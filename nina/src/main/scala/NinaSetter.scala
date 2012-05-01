package nina.impl

import java.math.BigDecimal
import java.util.Date

import java.sql.{PreparedStatement, Timestamp}

object NinaSetter {
	private def setDef[A](s: PreparedStatement, index: Int, value: A) { value match {
		case bd: BigDecimal => s.setBigDecimal(index, bd)
		case date: Date => s.setTimestamp(index, new Timestamp(date.getTime()))
		case obj => s.setObject(index, obj)
	}}
	def set[A](s: PreparedStatement, index: Int, value: A) { value match {
		case Some(x) => setDef(s, index, x)
		case None => setDef(s, index, null)
		case x => setDef(s, index, x)
	}}
}