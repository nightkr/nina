package object nina {
	implicit def table2query[T <: Table](t: T) = Query(t, Seq[Filter[_, T]]())

	implicit def anyToStatement[T] = anorm.ToStatement.anyParameter[T]
	implicit val dateToStatement = anorm.ToStatement.dateToStatement
	implicit def optionToStatement[T](implicit ts: anorm.ToStatement[T]) = anorm.ToStatement.optionToStatement[T](ts)
}