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

import busymachines.pureharm.anomaly.InconsistentStateCatastrophe
import busymachines.pureharm.db._
import busymachines.pureharm.db.flyway.FlywayConfig
import busymachines.pureharm.effects._
import busymachines.pureharm.effects.implicits._
import busymachines.pureharm.testkit._
import busymachines.pureharm.testkit.util._

/** @author
  *   Lorand Szakacs, https://github.com/lorandszakacs
  * @since 25
  *   Jun 2020
  */
trait DBTestSetup[DBTransactor] extends PureharmTestRuntimeLazyConversions {
  final type RT = PureharmTestRuntime

  implicit class TestSetupClassName(config: DBConnectionConfig) {

    /** @see
      *   schemaName
      */
    def withSchemaFromClassAndTest(testOptions: TestOptions): DBConnectionConfig =
      config.copy(schema = Option(schemaName(testOptions)))

    /** @see
      *   schemaName
      */
    def withSchemaFromClassAndTest(prefix:      String, testOptions: TestOptions): DBConnectionConfig =
      config.copy(schema = Option(schemaName(prefix, testOptions)))
  }

  /** Should be overridden to create a connection config appropriate for the test
    *
    * To ensure unique schema names for test cases use the extension methods:
    * TestSetupClassName.withSchemaFromClassAndTest or the explicit variants schemaName
    */
  def dbConfig(testOptions: TestOptions)(implicit logger: TestLogger): DBConnectionConfig

  def flywayConfig(testOptions: TestOptions)(implicit logger: TestLogger): Option[FlywayConfig] =
    dbConfig(testOptions).schema.map(dbSchema => FlywayConfig.defaultConfig.copy(schemas = List(dbSchema)))

  protected def dbTransactorInstance(
    testOptions:                TestOptions
  )(implicit rt:                RT, logger:                   TestLogger): Resource[IO, DBTransactor]

  def transactor(testOptions: TestOptions)(implicit rt: RT, logger: TestLogger): Resource[IO, DBTransactor] =
    for {
      _ <- logger.info(MDCKeys.testSetup(testOptions))("SETUP — init").to[Resource[IO, *]]
      schema = dbConfig(testOptions).schema.getOrElse("public")
      _       <-
        logger
          .info(MDCKeys.testSetup(testOptions) ++ Map("schema" -> schema))(s"SETUP — schema name for test: $schema")
          .to[Resource[IO, *]]
      _       <- _cleanDB(testOptions)
      _       <- _initDB(testOptions)
      fixture <- dbTransactorInstance(testOptions)
    } yield fixture

  protected def _initDB(testOptions: TestOptions)(implicit rt: RT, logger: TestLogger): Resource[IO, Unit] =
    for {
      _    <- logger.info(MDCKeys.testSetup(testOptions))("SETUP — preparing DB").to[Resource[IO, *]]
      att  <- flyway.Flyway
        .migrate[IO](dbConfig = dbConfig(testOptions), flywayConfig(testOptions))
        .timedAttempt()
        .to[Resource[IO, *]]
      (duration, migsAtt) = att
      migs <- migsAtt.liftTo[Resource[IO, *]]
      _    <- (migs <= 0).ifTrueRaise[Resource[IO, *]](
        InconsistentStateCatastrophe(
          """
            |Number of migrations run is 0.
            |
            |Meaning that the migrations are not in the proper folder.
            |
            |Please make sure to move them to the appropriate location corresponding to
            |where your test is. This is a common mistake... in Intellij it doesn't matter
            |in which module the migrations are... but it matters for SBT
            |
            |That's why you probably didn't encounter this damn bug so soon.
            |""".stripMargin
        )
      )

      _ <- logger.info(MDCKeys.testSetup(testOptions, duration))("SETUP — done preparing DB").to[Resource[IO, *]]
    } yield ()

  protected def _cleanDB(meta: TestOptions)(implicit rt: RT, logger: TestLogger): Resource[IO, Unit] =
    for {
      _ <- logger.info(MDCKeys.testSetup(meta))("SETUP — cleaning DB for a clean slate").to[Resource[IO, *]]
      _ <- flyway.Flyway.clean[IO](dbConfig(meta)).to[Resource[IO, *]]
      _ <- logger.info(MDCKeys.testSetup(meta))("SETUP — done cleaning DB").to[Resource[IO, *]]
    } yield ()

  /** @return
    *   The schema name in the format of: $${getClass.SimpleName()_$${testLineNumber}}
    */
  def schemaName(testOptions: TestOptions): SchemaName =
    truncateSchemaName(SchemaName(s"${schemaNameFromClassAndLineNumber(testOptions)}"))

  /** @return
    *   The schema name in the format of: prefix_{getClass.SimpleName()_{testLineNumber}}
    */
  def schemaName(prefix: String, testOptions: TestOptions): SchemaName =
    truncateSchemaName(SchemaName(s"${prefix}_${schemaNameFromClassAndLineNumber(testOptions)}"))

  protected def truncateSchemaName(s: SchemaName): SchemaName = SchemaName(s.takeRight(63))

  protected def schemaNameFromClassAndLineNumber(meta: TestOptions): SchemaName =
    SchemaName(s"${schemaNameFromClass}_${meta.location.line.toString}")

  protected def schemaNameFromClass: String =
    getClass.getSimpleName.replace("$", "").toLowerCase
}
