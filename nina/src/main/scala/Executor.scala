package nina

import java.sql.Connection

trait NinaExecutor {
	def update(table: String, filters: Seq[Filter[_, _]], data: Map[String, Any])(implicit conn: Connection): Int
	def delete(table: String, filters: Seq[Filter[_, _]])(implicit conn: Connection): Int
	def insert(table: String, data: Map[String, Any])(implicit conn: Connection): Boolean
	def count(table: String, filters: Seq[Filter[_, _]])(implicit conn: Connection): Long
	def getOne(table: String, filters: Seq[Filter[_, _]], columns: Seq[String])(implicit conn: Connection): Option[Map[String, Option[Any]]]
	def getMultiple(table: String, filters: Seq[Filter[_, _]], columns: Seq[String], amount: Long = -1)(implicit conn: Connection): Seq[Map[String, Option[Any]]]
}