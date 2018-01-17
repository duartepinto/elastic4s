package com.sksamuel.elastic4s.search.sort

import com.sksamuel.elastic4s.searches.sort.ScriptSortType
import com.sksamuel.elastic4s.testkit.{DockerTests, ElasticMatchers}
import org.scalatest.FreeSpec

import scala.util.Try

class ScriptSortTest extends FreeSpec with ElasticMatchers with DockerTests {

  Try {
    http.execute {
      deleteIndex("scriptsort")
    }.await
  }

  http.execute {
    createIndex("scriptsort").mappings(
      mapping("tubestops").fields(
        textField("name").fielddata(true),
        textField("line").fielddata(true)
      )
    )
  }.await

  http.execute {
    bulk(
      indexInto("scriptsort/tubestops") fields("name" -> "south kensington", "line" -> "district"),
      indexInto("scriptsort/tubestops") fields("name" -> "earls court", "line" -> "district"),
      indexInto("scriptsort/tubestops") fields("name" -> "bank", "line" -> "northern")
    ).refreshImmediately
  }.await

  "script sort" - {
    "sort by name length" in {
      val sorted = http.execute {
        search("scriptsort") query matchAllQuery sortBy {
          scriptSort(
            script(""" doc['name'].value.length() """)
          ) typed ScriptSortType.NUMBER
        }
      }.await.right.get.result
      sorted.hits.hits(0).sourceAsMap("name") shouldBe "bank"
      sorted.hits.hits(1).sourceAsMap("name") shouldBe "earls court"
      sorted.hits.hits(2).sourceAsMap("name") shouldBe "south kensington"
    }
  }

}
