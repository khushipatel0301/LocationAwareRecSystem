package NonSpatialRatingsForSpatial

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import utils.{CosineSimilarityOperation, OperationUtil}

import scala.util.control.Breaks._

object TravelPenaltyOperations {

  def calculateTravelPenaltyForDataFrame(dataFrame: DataFrame, maxDistance : Double, maxRating : Double) : DataFrame = {

    val distances : Array[Double] = dataFrame.select("distance").rdd.map(r => r(0).asInstanceOf[Double]).collect()
    val items : Array[String] = dataFrame.select("item").rdd.map(r => r(0).asInstanceOf[String]).collect()

    var travelPenalties : Array[Double] = new Array[Double](distances.length)

    var updatedDataFrame : DataFrame = dataFrame

    for( i <- 0 to (distances.length - 1) ) {
      travelPenalties(i) = maxRating * distances(i) / maxDistance
      updatedDataFrame = updatedDataFrame.withColumn("travelPenalty", when(col("item") === items(i), travelPenalties(i)).otherwise(col("travelPenalty")))
    }

    updatedDataFrame
  }

  def calculateRecScore(rating : Double, travelPenalty : Double) : Double = {
    val recScore : Double = rating - travelPenalty
    recScore
  }

  def calculateRecScoreForDataFrame(items: DataFrame, itemSimilarityMatrix : Array[Array[Double]], userItemMatrix : Array[Array[Double]], uniqueUsers : Array[String], totalUniqueItems : Integer): DataFrame = {

    val size : Long = items.count()
    val itemNames : Array[String] = items.select("item").rdd.map(r => r(0).asInstanceOf[String]).collect()
    val travelPenalties : Array[Double] = items.select("travelPenalty").rdd.map(r => r(0).asInstanceOf[Double]).collect()

    var updatedDataFrame : DataFrame = items

    for( i <- 0 to (itemNames.length-1)) {
      var rating : Double = 0
      var count : Integer = 0
      for( j <- 0 to (uniqueUsers.length -1)) {
        var tempRecScore : Double = 0
        if(userItemMatrix(j)(i) == -1) {
          var cfRating : Double = CosineSimilarityOperation.calculateCFBasedRating(itemSimilarityMatrix, userItemMatrix, j, i, totalUniqueItems)
          if(cfRating != 0) {
            tempRecScore = calculateRecScore(cfRating, travelPenalties(i))
            count = count + 1
          }
        } else {
          tempRecScore = calculateRecScore(userItemMatrix(j)(i), travelPenalties(i))
          count = count + 1
        }
        rating = rating + tempRecScore
      }
      rating = rating/count
      updatedDataFrame = updatedDataFrame.withColumn("recScore", when(col("item") === itemNames(i), rating).otherwise(col("recScore")))
    }

    updatedDataFrame.show()

    updatedDataFrame
  }

  def calculateKRecommendationFromTravelPenaltyOfDataFrame(sparkSession: SparkSession, items: DataFrame, k : Integer, lowestRecScore : Double, recommendation : DataFrame, maxRating : Double, itemSimilarityMatrix : Array[Array[Double]], userItemMatrix : Array[Array[Double]], totalUniqueItems : Integer, uniqueUsers : Array[String]) : DataFrame = {

    val itemNames : Array[String] = items.select("item").rdd.map(r => r(0).asInstanceOf[String]).collect()
    val travelPenalties : Array[Double] = items.select("travelPenalty").rdd.map(r => r(0).asInstanceOf[Double]).collect()

    var updatedRecommendationDf : DataFrame = recommendation
    var latestLowestRecScore : Double = lowestRecScore

    var tempRecScore : Double = 0

    items.show()
    items.createOrReplaceTempView("items")
    var updatedItems : DataFrame = sparkSession.sql("SELECT *, row_number() over (order by (select null)) as rowID FROM items")

    updatedItems.createOrReplaceTempView("updatedItems")

    breakable {

      for( i <- (k + 1 - 1) to (itemNames.length)) {

        tempRecScore = calculateRecScore(maxRating, travelPenalties(i))
        if (tempRecScore <= latestLowestRecScore) {
          break
        } else {
          var rating : Double = 0
          var count : Integer = 0
          for( j <- 0 to (uniqueUsers.length -1)) {
            var tempRecScore : Double = 0
            if(userItemMatrix(j)(i) == -1) {
              var cfRating : Double = CosineSimilarityOperation.calculateCFBasedRating(itemSimilarityMatrix, userItemMatrix, j, i, totalUniqueItems)
              if(cfRating != 0) {
                tempRecScore = calculateRecScore(cfRating, travelPenalties(i))
                count = count + 1
              }
            } else {
              tempRecScore = calculateRecScore(userItemMatrix(j)(i), travelPenalties(i))
              count = count + 1
            }
            rating = rating + tempRecScore
          }
          rating = rating/count

          if(rating > latestLowestRecScore) {

            updatedRecommendationDf.createOrReplaceTempView("recommendation")
            var recommendationDf : DataFrame = sparkSession.sql("SELECT *, row_number() over (order by recScore desc) as rowID FROM recommendation")

            recommendationDf.createOrReplaceTempView("UpdatedRecommendation")

            recommendationDf.collect().foreach(println)

            updatedRecommendationDf = sparkSession.sql("select * from UpdatedRecommendation WHERE rowID < " + k)

            updatedRecommendationDf = updatedRecommendationDf.drop("rowID")

            updatedRecommendationDf.collect().foreach(println)

            var row : DataFrame = sparkSession.sql("select * from updatedItems WHERE rowID = " + (i + 1))

            row = row.drop("rowID")

            row = row.withColumn("recScore", when(col("item") === itemNames(i), rating).otherwise(col("recScore")))

            row.collect().foreach(println)

            updatedRecommendationDf = updatedRecommendationDf.union(row)
            updatedRecommendationDf = updatedRecommendationDf.orderBy(col("recScore").desc)

            updatedRecommendationDf.show()

            latestLowestRecScore = OperationUtil.getLowestRecScore(sparkSession, updatedRecommendationDf)
          }

        }

      }

    }

    updatedRecommendationDf.select("item", "recScore")
  }

}
