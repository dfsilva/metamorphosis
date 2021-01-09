package br.com.diego.processor.repo

import com.github.tminglei.slickpg._
import slick.basic.Capability

import scala.concurrent.duration.Duration

trait PostgresProfile extends ExPostgresProfile
  with PgArraySupport
  with PgDate2Support
  with PgDateSupport
  with PgRangeSupport
  with PgHStoreSupport
  with PgCirceJsonSupport
  with PgSearchSupport
  with PgNetSupport
  with PgLTreeSupport {
  def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + slick.jdbc.JdbcCapabilities.insertOrUpdate

  override val api = MyAPI

  object MyAPI extends API with ArrayImplicits
    with DateTimeImplicits
    with SimpleDateTimeImplicits
    with JsonImplicits
    with NetImplicits
    with LTreeImplicits
    with RangeImplicits
    with HStoreImplicits
    with SearchImplicits
    with SearchAssistants {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)

    implicit val scDurationTypeMapper = MappedColumnType.base[Duration, String](
      { dur => dur.toString },
      { str => Duration(str) }
    )

    implicit val anyTypeMapper = MappedColumnType.base[Any, String](
      { dur => dur.toString },
      { str => str }
    )

  }

}

object PostgresProfile extends PostgresProfile
