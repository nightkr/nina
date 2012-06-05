package nina

import java.sql.{Connection, PreparedStatement}

package filter {
	sealed trait Kind
	case object Like extends Kind
	case object EQ extends Kind
	case object NEQ extends Kind
	case object GT extends Kind
	case object GTE extends Kind
	case object LT extends Kind
	case object LTE extends Kind
}

case class Filter[A, T <: Table](column: T#Column[A], kind: filter.Kind, other: A)

case class Query[T <: Table](table: T, filters: Seq[Filter[_, T]]) {
	def count(implicit conn: Connection) = table.executor.count(table.tableName, filters)

	def where[A](p: => (table.Column[A], nina.filter.Kind, A)): Query[T] = {
		val (col, kind, other) = p
		Query(table, filters :+ Filter(col, kind, other))
	}

	def get[A](cols: table.Columns[A]) = GetQuery(this, cols, None)
	/**
	  * @returns The amount of rows affected
	  */
	def set(values: table.SingleBoundColumn[_]*)(implicit conn: Connection) = {
		table.executor.update(table.tableName, filters, values.map {col => col.column.name -> col.value} toMap)
	}
	/**
	  * @returns The amount of rows affected
	  */
	def delete()(implicit conn: Connection) = {
		table.executor.delete(table.tableName, filters)
	}
}

sealed trait OrderDirection
case object Ascending extends OrderDirection
case object Descending extends OrderDirection

case class GetQuery[A, T <: Table](query: Query[T], cols: T#Columns[A], ordering: Option[(T#Column[_], OrderDirection)]) {
	private val stringifiedOrdering = ordering map { case (col, dir) => (col.name, dir) }

	def order(column: query.table.Column[_], direction: OrderDirection) = order((column, direction))
	def order(columnAndDirection: (query.table.Column[_], OrderDirection)) = GetQuery(query, cols, Some(columnAndDirection))

	def single()(implicit conn: Connection): Option[A] = query.table.executor.getOne(query.table.tableName, query.filters, cols.columnNames, stringifiedOrdering).map(cols.bindFromMap(_).value)
	def take(amount: Long)(implicit conn: Connection): Seq[A] = query.table.executor.getMultiple(query.table.tableName, query.filters, cols.columnNames, stringifiedOrdering, amount).map(cols.bindFromMap(_).value)
	def all()(implicit conn: Connection): Seq[A] = query.table.executor.getMultiple(query.table.tableName, query.filters, cols.columnNames, stringifiedOrdering).map(cols.bindFromMap(_).value)
}