package nina

import play.api.Play._

import java.sql.Connection

sealed trait Columns[A] {
	def columnNames: Seq[String]
	def bind(value: A): BoundColumn[A]
	def bindFromMap(map: Map[String, Option[Any]]): BoundColumn[A]
}

trait BoundColumn[A] {
	def column: Columns[A]
	def value: A
}

case class &[+A, +B](_1: A, _2: B)

trait Table {
	def tableName: String

	def executor: NinaExecutor = {
		val executor = configuration getString "nina.executor" getOrElse "mysql" match {
			case "mysql" => "nina.executors.MySQL"
			case x => x
		}
		val clazz = classloader.loadClass(executor+"$")
		clazz.getField("MODULE$").get(null).asInstanceOf[NinaExecutor]
	}

	case class BoundColumn[A](column: Columns[A], value: A) extends nina.BoundColumn[A]
	case class SingleBoundColumn[A](column: Column[A], value: A) extends nina.BoundColumn[A]

	sealed trait Columns[A] extends nina.Columns[A] {
		def &[B](other: Columns[B]) = CompositeColumn[A, B](this, other)
	}
	sealed trait Column[A] extends Columns[A] {
		def name: String
		val columnNames = Seq(name)
		def bind(value: A) = BoundColumn(this, value)
		def :=(value: A) = SingleBoundColumn(this, value)

		def like(other: A): (Column[A], filter.Kind, A) = (this, filter.Like, other)
		def same(other: A): (Column[A], filter.Kind, A) = (this, filter.EQ, other)
		def nsame(other: A): (Column[A], filter.Kind, A) = (this, filter.NEQ, other)
		def gt(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.GT, other)
		def gte(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.GTE, other)
		def lt(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.LT, other)
		def lte(other: A)(implicit num: Numeric[A]): (Column[A], filter.Kind, A) = (this, filter.LTE, other)

		def ~= = like _
		def === = same _
		def !== = nsame _
		def >(implicit num: Numeric[A]) = gt _
		def >=(implicit num: Numeric[A]) = gte _
		def <(implicit num: Numeric[A]) = lt _
		def <=(implicit num: Numeric[A]) = lte _
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

	def optCol[A](name: String): Column[Option[A]] = NullableColumn[A](name)
	def col[A](name: String): Column[A] = UnnullableColumn[A](name)

	def insert(data: SingleBoundColumn[_]*)(implicit conn: Connection) = executor.insert(this.tableName, data map {col => (col.column.name, col.value)} toMap)
}

trait PrimaryKey[A] { self: Table =>
	def pk: Column[A]
}