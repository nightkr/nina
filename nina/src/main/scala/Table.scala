package nina

import java.sql.Connection

/**
  * One or more columns.
  */
sealed trait Columns[A] {
	/**
	  * The name of the column, if a single column, or the sum of the child column names for a composite column.
	  */
	def columnNames: Seq[String]
	/**
	  * Create a [[nina.BoundColumn]] from the given value.
	  */
	def bind(value: A): BoundColumn[A]
	/**
	  * Bind to the values from the given map.
	  *
	  * @param map A map of (column, value).
	  */
	def bindFromMap(map: Map[String, Option[Any]]): BoundColumn[A]
}

/**
  * A combination of one or more [[nina.Columns]] and a corresponding value.
  *
  * @tparam A The type of the value
  */
trait BoundColumn[A] {
	def column: Columns[A]
	def value: A
}

/**
  * A combination of two values, used for combining the values of two [[nina.Columns]].
  */
case class &[+A, +B](_1: A, _2: B)

/**
  * The base class for templates. All tables should be singletons that derive from this.
  *
  * Example:
  * {{{
object Languages extends Table("languages") with PrimaryKey[Int] {
	val pk = id

	val id = col[Int]("id")
	val name = col[String]("name")
}
    }}}

  * To query/update/delete data, use the implicit conversion to [[nina.Query]].
  */
abstract class Table(val tableName: String)(implicit val executor: NinaExecutor) {
	case class BoundColumn[A](column: Columns[A], value: A) extends nina.BoundColumn[A]
	case class SingleBoundColumn[A](column: Column[A], value: A) extends nina.BoundColumn[A]

	sealed trait Columns[A] extends nina.Columns[A] {
		def &[B](other: Columns[B]) = CompositeColumn[A, B](this, other)
	}
	/**
	  * A single column.
	  */
	sealed trait Column[A] extends Columns[A] {
		def name: String
		val columnNames = Seq(name)
		def bind(value: A) = BoundColumn(this, value)
		/**
		  * Bind this single column to a value.
		  */
		def :=(value: A) = SingleBoundColumn(this, value)

		/**
		  * Equivalent to the SQL LIKE operator.
		  */
		def like(other: A): (Column[A], filter.Kind, A) = (this, filter.Like, other)
		/**
		  * Equivalent to the SQL = comparison operator.
		  */
		def same(other: A): (Column[A], filter.Kind, A) = (this, filter.EQ, other)
		/**
		  * Equivalent to the SQL != operator.
		  */
		def nsame(other: A): (Column[A], filter.Kind, A) = (this, filter.NEQ, other)
		/**
		  * Equivalent to the SQL > operator.
		  */
		def gt(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.GT, other)
		/**
		  * Equivalent to the SQL >= operator.
		  */
		def gte(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.GTE, other)
		/**
		  * Equivalent to the SQL < operator.
		  */
		def lt(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.LT, other)
		/**
		  * Equivalent to the SQL >= operator.
		  */
		def lte(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.LTE, other)

		/** @see like */ def ~= = like _
		/** @see same */ def === = same _
		/** @see nsame */ def !== = nsame _
		/** @see gt */ def >(implicit num: Numeric[A]) = gt _
		/** @see gte */ def >=(implicit num: Numeric[A]) = gte _
		/** @see lt */ def <(implicit num: Numeric[A]) = lt _
		/** @see lte */ def <=(implicit num: Numeric[A]) = lte _
	}
	private case class NullableColumn[A](name: String) extends Column[Option[A]] {
		def bindFromMap(map: Map[String, Option[Any]]) = bind(map(name).map(_.asInstanceOf[A]))
	}
	private case class UnnullableColumn[A](name: String) extends Column[A] {
		def bindFromMap(map: Map[String, Option[Any]]) = bind(map(name).get.asInstanceOf[A])
	}
	case class CompositeColumn[A, B](_1: Columns[A], _2: Columns[B]) extends Columns[A & B] {
		val columnNames = _1.columnNames ++ _2.columnNames
		def bind(value: A & B) = BoundColumn(this, nina.&(_1.bind(value._1).value, _2.bind(value._2).value))
		def bindFromMap(map: Map[String, Option[Any]]) = BoundColumn(this, nina.&(_1.bindFromMap(map).value, _2.bindFromMap(map).value))
	}

	/**
	  * Declare a new nullable column.
	  */
	def optCol[A](name: String): Column[Option[A]] = NullableColumn[A](name)
	/**
	  * Declare a new non-nullable column.
	  */
	def col[A](name: String): Column[A] = UnnullableColumn[A](name)

	/**
	  * Insert the given columns into the table as a new row.
	  */
	def insert(data: SingleBoundColumn[_]*)(implicit conn: Connection) = executor.insert(this.tableName, data map {col => (col.column.name, col.value)} toMap)
}

/**
  * Mixin trait for [[nina.Table]]s with primary keys.
  *
  * Currently unused, later on tables with primary keys will implicitly convert to their primary key.
  */
trait PrimaryKey[A] { self: Table =>
	def pk: Column[A]
}