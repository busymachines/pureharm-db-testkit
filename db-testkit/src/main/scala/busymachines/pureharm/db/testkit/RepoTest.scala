/*
 * Copyright 2019 BusyMachines
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package busymachines.pureharm.db.testkit

import busymachines.pureharm.db._
import busymachines.pureharm.effects._
import busymachines.pureharm.effects.implicits._

/** @author Lorand Szakacs, https://github.com/lorandszakacs
  * @since 25 Jun 2020
  */
@scala.annotation.nowarn // remove once we remove deprecated methods from Repo.
abstract class RepoTest[E, PK, Trans](implicit show: Show[PK]) extends DBTest[Trans] {
  override type ResourceType <: Repo[IO, E, PK]

  def data: RepoTestData[E, PK]

  protected def dataAssumptionCheck: Resource[IO, Unit] = Resource.eval[IO, Unit] {
    IO.delay {
      assume(data.pk1 != data.pk2, "Incorrect test data. primary keys have to be different for the two rows")
      assume(data.nonExistentPK != data.pk1, "Incorrect test data. nonExistentPK has to be different from PK1")
      assume(data.nonExistentPK != data.pk2, "Incorrect test data. nonExistentPK has to be different from PK2")
    }
  }

  override def testResource = ResourceFixture { testOptions =>
    for {
      _     <- dataAssumptionCheck
      trans <- setup.transactor(testOptions)
      fix   <- resource(testOptions, trans)
    } yield fix
  }

  testResource.test("find nonExistentPK -> none") { implicit repo: ResourceType =>
    for {
      att <- repo.find(data.nonExistentPK)
    } yield assert(att.isEmpty)
  }

  testResource.test("retrieve nonExistentPK —> failed") { implicit repo: ResourceType =>
    for {
      att <- repo.retrieve(data.nonExistentPK).attempt
      _ = interceptFailure[DBEntryNotFoundAnomaly](att)
    } yield ()
  }

  testResource.test("exists in empty DB -> false") { implicit repo: ResourceType =>
    for {
      exists <- repo.exists(data.pk1)
    } yield assert(!exists)
  }

  testResource.test("insert row1 + find -> some") { implicit repo: ResourceType =>
    for {
      _          <- repo.insert(data.row1)
      fetchedRow <- repo.find(data.pk1).flattenOption(fail(s"PK=${data.pk1} row was not in database"))
    } yield assertEquals(obtained = fetchedRow, expected = data.row1)
  }

  testResource.test("insert row1 + row -> duplicate primary key") { implicit repo: ResourceType =>
    for {
      _          <- repo.insert(data.row1)
      fetchedRow <- repo.find(data.pk1).flattenOption(fail(s"PK=${data.pk1} row was not in database"))
      _ = assertEquals(obtained = fetchedRow, expected = data.row1)
      attempt <- repo.insert(data.row1).attempt
      failure = interceptFailure[DBUniqueConstraintViolationAnomaly](attempt)
    } yield {
      assert(failure.column == data.iden.fieldName, "pk column in error")
      assert(failure.value == data.pk1.show, "duplicate value in error")
    }
  }

  testResource.test("insert row1 + retrieve -> success") { implicit repo: ResourceType =>
    for {
      _          <- repo.insert(data.row1)
      fetchedRow <- repo.retrieve(data.pk1)
    } yield assertEquals(obtained = fetchedRow, expected = data.row1)
  }

  testResource.test("insert row1 + exists -> true") { implicit repo: ResourceType =>
    for {
      _      <- repo.insert(data.row1)
      exists <- repo.exists(data.pk1)
    } yield assert(exists)
  }

  testResource.test("insert row1 + delete + find -> none") { implicit repo: ResourceType =>
    for {
      _       <- repo.insert(data.row1)
      _       <- repo.delete(data.pk1)
      deleted <- repo.find(data.pk1)
      _ = assert(deleted.isEmpty)
      _ <- repo.insert(data.row1)
      r <- repo.find(data.pk1)
      _ = assertSome(r)(data.row1)

    } yield ()
  }

  testResource.test("insert row + idempotent update") { implicit repo: ResourceType =>
    for {
      _          <- repo.insert(data.row1)
      _          <- repo.update(data.row1)
      fetchedRow <- repo.retrieve(data.pk1)
    } yield assertEquals(obtained = fetchedRow, expected = data.row1)
  }

  testResource.test("insert row1 + row1Update1") { implicit repo: ResourceType =>
    for {
      _ <- repo.insert(data.row1)
      _        = assume(data.row1 != data.row2, "Incorrect test data. We need at least one row that's different from row1")
      toUpdate = data.row1Update1
      _          <- repo.update(toUpdate)
      fetchedRow <- repo.retrieve(data.pk1)
    } yield assertEquals(obtained = fetchedRow, expected = toUpdate)
  }

  if (data.isUniqueUpdate2) {
    testResource.test("insert row1 + row1Update2") { implicit repo: ResourceType =>
      for {
        _ <- repo.insert(data.row1)
        toUpdate = data.row1Update2
        _          <- repo.update(toUpdate)
        fetchedRow <- repo.retrieve(data.pk1)
      } yield assertEquals(obtained = fetchedRow, expected = toUpdate)
    }
  }

  if (data.isUniqueUpdate3) {
    testResource.test("insert row1 + row1Update3") { implicit repo: ResourceType =>
      for {
        _ <- repo.insert(data.row1)
        toUpdate = data.row1Update3
        _          <- repo.update(toUpdate)
        fetchedRow <- repo.retrieve(data.pk1)
      } yield assertEquals(obtained = fetchedRow, expected = toUpdate)
    }
  }

  if (data.isUniqueUpdate4) {
    testResource.test("insert row1 + row1Update4") { implicit repo: ResourceType =>
      for {
        _ <- repo.insert(data.row1)
        toUpdate = data.row1Update4
        _          <- repo.update(toUpdate)
        fetchedRow <- repo.retrieve(data.pk1)
      } yield assertEquals(obtained = fetchedRow, expected = toUpdate)
    }
  }

  testResource.test("insertMany row1, row2 + find") { implicit repo: ResourceType =>
    for {
      _  <- repo.insertMany(List(data.row1, data.row2))
      r1 <- repo.find(data.pk1)
      r2 <- repo.find(data.pk2)
    } yield {
      assert(r1.nonEmpty, "data.row1")
      assert(r2.nonEmpty, "data.row2")
    }
  }

  testResource.test("insertMany row1, row1 -> duplicate key on row1") { implicit repo: ResourceType =>
    for {
      att <- repo.insertMany(List(data.row1, data.row1)).attempt
      e = interceptFailure[DBBatchInsertFailedAnomaly](att)
      _ = assert(Option(e.getCause).nonEmpty, "insert should have cause")
      _ = interceptFailure[DBUniqueConstraintViolationAnomaly](e.getCause.asLeft)
      row1 <- repo.find(data.pk1)
    } yield assert(row1.isEmpty, "... on batch insert one failure, means all fail, but row1 was still added")
  }

  testResource.test("insertMany row1, row2 — deleteMany row1, row2 -> no rows should exist") {
    implicit repo: ResourceType =>
      for {
        _  <- repo.insertMany(List(data.row1, data.row2))
        _  <- repo.deleteMany(List(data.pk1, data.pk2))
        r1 <- repo.find(data.pk1)
        r2 <- repo.find(data.pk2)
      } yield {
        assert(r1.isEmpty, "r1 should have been deleted")
        assert(r2.isEmpty, "r2 should have been deleted")
      }
  }

  testResource.test("insertMany row1, row2 — deleteMany row1 -> row2 should exist") { implicit repo: ResourceType =>
    for {
      _  <- repo.insertMany(List(data.row1, data.row2))
      _  <- repo.deleteMany(List(data.pk1))
      r1 <- repo.find(data.pk1)
      r2 <- repo.find(data.pk2)
    } yield {
      assert(r1.isEmpty, "r1 should have been deleted")
      assert(r2.nonEmpty, "r2 should NOT have been deleted")
    }
  }

  testResource.test("insert row1, row2 — existsAtLeastOne row1, row2 -> true") { implicit repo: ResourceType =>
    for {
      _      <- repo.insertMany(List(data.row1, data.row2))
      exists <- repo.existsAtLeastOne(List(data.pk1))
    } yield assert(exists)
  }

  testResource.test("insert row1 — existsAtLeastOne row1, row2 -> true") { implicit repo: ResourceType =>
    for {
      _      <- repo.insertMany(List(data.row2))
      exists <- repo.existsAtLeastOne(List(data.pk1, data.pk2))
    } yield assert(exists)
  }

  testResource.test("insert row1, row2 — existsAll row1, row2 -> true") { implicit repo: ResourceType =>
    for {
      _         <- repo.insertMany(List(data.row1, data.row2))
      existsAll <- repo.existAll(List(data.pk1, data.pk2))
    } yield assert(existsAll)
  }

  testResource.test("insert row1, row2 — existsAll row1 -> true") { implicit repo: ResourceType =>
    for {
      _         <- repo.insertMany(List(data.row1, data.row2))
      existsAll <- repo.existAll(List(data.pk1))
    } yield assert(existsAll)
  }

  testResource.test("insert row1, row2 — existsAll row1, row2, nonExistent -> false") { implicit repo: ResourceType =>
    for {
      _         <- repo.insertMany(List(data.row1, data.row2))
      existsAll <- repo.existAll(List(data.pk1, data.pk2, data.nonExistentPK))
    } yield assert(!existsAll)
  }
}
