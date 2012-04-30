package nina.executors

import nina._

import java.sql.Connection

import anorm._

object MySQL extends NinaExecutor {
	def insert(table: String, data: Map[String, Any])(implicit conn: Connection): Boolean = (
		SQL("INSERT INTO "+table+"("+data.keys.mkString(",")+") VALUES("+data.keys.map("{"+_.toString+"}").mkString(",")+")")
		.on(data.toSeq.map { (kv) => val (key, value) = kv; (key, toParameterValue(value)) }: _*)
		.executeUpdate() > 0
	)

	def count(table: String, filters: Seq[Filter[_, _]])(implicit conn: Connection): Long = getOne(table, filters, Seq("count(*)")).get("count(*)").asInstanceOf[Option[Long]].get
	def getOne(table: String, filters: Seq[Filter[_, _]], columns: Seq[String])(implicit conn: Connection): Option[Map[String, Option[Any]]] = {
		getMultiple(table, filters, columns, 1).headOption
	}
	def getMultiple(table: String, filters: Seq[Filter[_, _]], columns: Seq[String], amount: Long)(implicit conn: Connection): Seq[Map[String, Option[Any]]] = {
		val filtersStr = if (filters.isEmpty) ""
						 else " WHERE "+filters.map(_ match {
						 	case Filter(col, filter.Like, _) => col.name+" LIKE {"+col.name+"}"
							case Filter(col, filter.EQ, _) => col.name+"={"+col.name+"}"
							case Filter(col, filter.NEQ, _) => col.name+"!={"+col.name+"}"
							case Filter(col, filter.GT, _) => col.name+">{"+col.name+"}"
							case Filter(col, filter.GTE, _) => col.name+">={"+col.name+"}"
							case Filter(col, filter.LT, _) => col.name+"<{"+col.name+"}"
							case Filter(col, filter.LTE, _) => col.name+"<={"+col.name+"}"
						 }).mkString(" AND ")
		val limitStr = if (amount == -1) ""
					   else " LIMIT "+amount
		val sql = "SELECT "+columns.mkString(",")+" FROM "+table+filtersStr+limitStr
		val rows = SQL(sql).on(filters.map(_ match {
			case filter: Filter[_, _] => filter.column.name -> ParameterValue(filter.other, filter.setter)
		}): _*)()

		var allRows = Seq[Row]() // Hack to make sure that all rows are fetched
		for (row <- rows) {
			allRows :+= row
		}

		allRows map { row =>
			columns.map[(String, Option[Any]), Seq[(String, Option[Any])]] { name =>
				(name, row[Option[Any]](name)(Column.rowToOption(Column { (value, metadata) =>
					Right(value)
				})))
			} toMap
		}
	}
}