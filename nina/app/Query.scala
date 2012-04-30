package nina

import java.sql.Connection

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

case class Filter[A, T <: Table](column: T#Column[A], kind: filter.Kind, other: A)(implicit val setter: anorm.ToStatement[A])

case class Query[T <: Table](table: T, filters: Seq[Filter[_, T]]) {
	def count(implicit conn: Connection) = table.executor.count(table.tableName, filters)

	def where[A](p: => (table.Column[A], nina.filter.Kind, A))(implicit setter: anorm.ToStatement[A]): Query[T] = {
		val (col, kind, other) = p
		Query(table, filters :+ Filter(col, kind, other))
	}

	def get[A](cols: table.Columns[A]) = GetQuery(this, cols, Seq())
}

case class GetQuery[A, T <: Table](query: Query[T], cols: T#Columns[A], lazyFilters: Seq[A => Boolean]) {
	def withFilter[B](p: A => Boolean)(implicit setter: anorm.ToStatement[A], bNullable: Null <:< B): GetQuery[A, T] = GetQuery(query, cols, lazyFilters :+ p)

	def single()(implicit conn: Connection): Option[A] = query.table.executor.getOne(query.table.tableName, query.filters, cols.columnNames).map(cols.bindFromMap(_).value)
	def take(amount: Long)(implicit conn: Connection): Seq[A] = query.table.executor.getMultiple(query.table.tableName, query.filters, cols.columnNames, amount).map(cols.bindFromMap(_).value)
	def all()(implicit conn: Connection): Seq[A] = query.table.executor.getMultiple(query.table.tableName, query.filters, cols.columnNames).map(cols.bindFromMap(_).value)
}