package test

import nina._

import org.specs2.mutable._

import play.api.Play

import play.api.test._
import play.api.test.Helpers._

import play.api.db.DB

class TableSpec extends Specification {
	object TestingTable extends Table with PrimaryKey[Int] {
		val tableName = "languages"

		val id = col[Int]("id")
		val name = col[String]("name")
		val rating = optCol[Int]("rating")

		val pk = id

		val creationSQL = """
		CREATE TABLE """+tableName+"""(
			id INTEGER AUTO_INCREMENT NOT NULL,
			name VARCHAR(255) NOT NULL,
			rating INTEGER,
			PRIMARY KEY(id)
		);

		INSERT INTO """+tableName+"""(name, rating) VALUES('Java', 1);
		INSERT INTO """+tableName+"""(name) VALUES('Guava');
		INSERT INTO """+tableName+"""(name, rating) VALUES('Haskell', -1);
		"""
	}

	"A table" should {
		"be able to add rows" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				import Play.current
				DB.withConnection { implicit c =>
					anorm.SQL(TestingTable.creationSQL).execute()
					val rows = TestingTable.count
					TestingTable insert (TestingTable.name := "Scala")
					TestingTable.count must equalTo(rows+1)
				}
			}
		}

		"be filterable" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				import Play.current
				DB.withConnection { implicit c =>
					anorm.SQL(TestingTable.creationSQL).execute()
					var count = 0
					for (name <- TestingTable where (TestingTable.name ~= "%ava") get TestingTable.name all()) {
						count += 1
						name must endWith("ava")
					}
					count must equalTo(2)
				}
			}
		}

		"handle nullable items and composite columns correctly" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				import Play.current
				DB.withConnection { implicit c =>
					anorm.SQL(TestingTable.creationSQL).execute()
					var count = 0
					for (name & rating <- TestingTable get (TestingTable.name & TestingTable.rating) all()) {
						count += 1
						rating must equalTo(name match {
							case "Java" => Some(1)
							case "Guava" => None
							case "Haskell" => Some(-1)
						})
					}
					count must equalTo(3)
				}
			}
		}

		"be able to only query for a limited amount of results" in {
			running(FakeApplication(additionalConfiguration = inMemoryDatabase())) {
				import Play.current
				DB.withConnection { implicit c =>
					anorm.SQL(TestingTable.creationSQL).execute()
					var count = 0
					for (name <- TestingTable get TestingTable.name take 2) {
						count += 1
					}
					count must equalTo(2)
				}
			}
		}
	}
}