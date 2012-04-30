package nina.impl.play

import nina._

import java.sql.Connection

import anorm._

package object mysql extends NinaPlayImplPackage {
	implicit val ninaPlayMySQLExecutor: NinaExecutor = NinaPlayMySQLExecutor
}

package mysql {
	object NinaPlayMySQLExecutor extends NinaExecutor {
		def serializeFilter(filters: Seq[Filter[_, _]]) = if (filters.isEmpty) ""
														  else " WHERE "+filters.map(_ match {
							 								case Filter(col, filter.Like, _) => col.name+" LIKE {Like_"+col.name+"}"
															case Filter(col, filter.EQ, _) => col.name+"={EQ_"+col.name+"}"
															case Filter(col, filter.NEQ, _) => col.name+"!={NEQ_"+col.name+"}"
															case Filter(col, filter.GT, _) => col.name+">{GT_"+col.name+"}"
															case Filter(col, filter.GTE, _) => col.name+">={GTE_"+col.name+"}"
															case Filter(col, filter.LT, _) => col.name+"<{LT_"+col.name+"}"
															case Filter(col, filter.LTE, _) => col.name+"<={LTE_"+col.name+"}"
													 	  }).mkString(" AND ")

		def update(table: String, filters: Seq[Filter[_, _]], data: Map[String, Any])(implicit conn: Connection) = {
			val dataStr = if (data.isEmpty) ""
			              else " SET "+(data.keys.map { name => name+"={val_"+name+"}" } mkString(","))
			SQL("UPDATE "+table+dataStr+serializeFilter(filters)).on((
				data.toSeq.map { kv => val (key, value) = kv; "val_"+key -> toParameterValue(value) } ++
				filters.toSeq.map(_ match { case filter: Filter[_, _] => filter.kind.toString+"_"+filter.column.name -> ParameterValue(filter.other, filter.setter) })
			): _*).executeUpdate()
		}
		def delete(table: String, filters: Seq[Filter[_, _]])(implicit conn: Connection): Int = {
			SQL("DELETE FROM "+table+serializeFilter(filters)).on(filters.map(_ match {
				case filter: Filter[_, _] => filter.kind.toString+"_"+filter.column.name -> ParameterValue(filter.other, filter.setter)
			}): _*).executeUpdate()
		}
		def insert(table: String, data: Map[String, Any])(implicit conn: Connection): Boolean = (
			SQL("INSERT INTO "+table+"("+data.keys.mkString(",")+") VALUES("+data.keys.map("{"+_.toString+"}").mkString(",")+")")
			.on(data.toSeq.map { kv => val (key, value) = kv; key -> toParameterValue(value) }: _*)
			.executeUpdate() > 0
		)

		def count(table: String, filters: Seq[Filter[_, _]])(implicit conn: Connection): Long = getOne(table, filters, Seq("count(*)")).get("count(*)").asInstanceOf[Option[Long]].get
		def getOne(table: String, filters: Seq[Filter[_, _]], columns: Seq[String])(implicit conn: Connection): Option[Map[String, Option[Any]]] = {
			getMultiple(table, filters, columns, 1).headOption
		}
		def getMultiple(table: String, filters: Seq[Filter[_, _]], columns: Seq[String], amount: Long)(implicit conn: Connection): Seq[Map[String, Option[Any]]] = {
			val limitStr = if (amount == -1) ""
						   else " LIMIT "+amount
			val sql = "SELECT "+columns.mkString(",")+" FROM "+table+serializeFilter(filters)+limitStr
			val rows = SQL(sql).on(filters.map(_ match {
				case filter: Filter[_, _] => filter.kind.toString+"_"+filter.column.name -> ParameterValue(filter.other, filter.setter)
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
}