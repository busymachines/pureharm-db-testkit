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

package busymachines.pureharm.db

import java.util.UUID
import busymachines.pureharm.effects._
import busymachines.pureharm.effects.implicits._
import busymachines.pureharm.sprout._

/** @author
  *   Lorand Szakacs, https://github.com/lorandszakacs
  * @since 13
  *   Jun 2019
  */
package object testdata {

  object SproutByte extends Sprout[Byte]
  type SproutByte = SproutByte.Type

  object SproutInt extends Sprout[Int]
  type SproutInt = SproutInt.Type

  object SproutLong extends Sprout[Long]
  type SproutLong = SproutLong.Type

  object SproutBigDecimal extends Sprout[BigDecimal]
  type SproutBigDecimal = SproutBigDecimal.Type

  object SproutString extends Sprout[String]
  type SproutString = SproutString.Type

  object SproutPK extends Sprout[String] {
    implicit val showPK: Show[this.Type] = Show[String].contramap(oldType)
  }
  type SproutPK = SproutPK.Type

  object UniqueString extends Sprout[String]
  type UniqueString = UniqueString.Type

  object UniqueInt extends Sprout[Int]
  type UniqueInt = UniqueInt.Type

  object UniqueJSON extends Sprout[PHJSONCol]
  type UniqueJSON = UniqueJSON.Type

  object SproutUUID extends Sprout[UUID] {
    def unsafeFromString(s: String):      SproutUUID = this(UUID.fromString(s))
    def unsafeFromBytes(a:  Array[Byte]): SproutUUID = this(UUID.nameUUIDFromBytes(a))

    def unsafeGenerate: SproutUUID = this(UUID.randomUUID())
    def generate[F[_]: Sync]: F[SproutUUID] = Sync[F].delay(unsafeGenerate)

    implicit val showUUID: Show[SproutUUID] = Show.fromToString[SproutUUID]
  }
  type SproutUUID = SproutUUID.Type

  object schema {
    val PureharmRows:         TableName = TableName("pureharm_rows")
    val PureharmExternalRows: TableName = TableName("pureharm_external_rows")
  }

}
