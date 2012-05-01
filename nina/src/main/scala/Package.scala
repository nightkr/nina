package object nina {
	implicit def table2query[T <: Table](t: T) = Query(t, Seq[Filter[_, T]]())
	implicit def pk2column[A, T <: Table with PrimaryKey[A]](table: T): T#Column[A] = table.pk
}