package object nina {
	implicit def table2query[T <: Table](t: T) = Query(t, Seq[Filter[_, T]]())
}