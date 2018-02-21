package utils

import org.apache.spark.sql.{DataFrame, SparkSession}

object CosineSimilarityOperation {

  def populateUserItemRatingMatrix(sparkSession: SparkSession, dataFrame: DataFrame, userItemRatings : Array[Array[Double]], userIDs : Array[String], items : Array[String]) : Array[Array[Double]] = {

    var userItemPopulatedRatings = userItemRatings

    dataFrame.createOrReplaceTempView("input")

    for(i <- 0 to (userIDs.length-1)) {
      for(j <- 0 to (items.length-1)) {
        var temp : DataFrame = sparkSession.sql("SELECT ratings FROM input WHERE userID = '" + userIDs(i) + "' AND item ='" + items(j) + "'")
        if(temp.count() == 0) {
          userItemPopulatedRatings(i)(j) = -1
        } else {
          userItemPopulatedRatings(i)(j) = temp.head().toString().substring(1, temp.head().toString().length - 1).toDouble
        }
      }
    }

    for(i <- 0 to (userIDs.length-1)) {
      for (j <- 0 to (items.length - 1)) {
        print(userItemPopulatedRatings(i)(j) + "  ")
      }
      println(" ")
    }

    userItemPopulatedRatings
  }

  def createSimilarityMatrix(sparkSession: SparkSession, dataFrame: DataFrame, similarityMatrix : Array[Array[Double]], userItemRatings : Array[Array[Double]], totalUniqueItems : Integer, totalUniqueUsers : Integer) : Array[Array[Double]] = {

    var itemSimilarityMatrix = similarityMatrix

    for (i <- 0 to (totalUniqueItems-1)) {
      for (j <- 0 to (totalUniqueItems - 1)) {
        var similarity : Double = 0
        var temp1 : Double = 0
        var temp2 : Double = 0
        var flag : Boolean = false
        if(i == j) {
          itemSimilarityMatrix(i)(j) = 1
        } else {
          for (m <- 0 to (totalUniqueUsers - 1)) {
            if( userItemRatings(m)(i) != -1 && userItemRatings(m)(j) != -1 ) {
              flag = true
              similarity = similarity + (userItemRatings(m)(i) * userItemRatings(m)(j))
              temp1 = temp1 + (userItemRatings(m)(i)*userItemRatings(m)(i))
              temp2 = temp2 + (userItemRatings(m)(j)*userItemRatings(m)(j))
            }
          }
          if(flag == true) {
            similarity = similarity / (scala.math.sqrt(temp1) * scala.math.sqrt(temp2))
            itemSimilarityMatrix(i)(j) = similarity
          }
        }
      }
    }

    for (i <- 0 to (totalUniqueItems-1)) {
      for (j <- 0 to (totalUniqueItems - 1)) {
        print(itemSimilarityMatrix(i)(j) + "  ")
      }
      println(" ")
    }

    itemSimilarityMatrix
  }

  def calculateCFBasedRating(itemSimilarityMatrix : Array[Array[Double]], userItemMatrix : Array[Array[Double]], userID : Integer, item : Integer, totalUniqueItems : Integer): Double = {

    var rating : Double = 0
    var totalSimilarity : Double = 0

    for(i <- 0 to (totalUniqueItems - 1)) {


        if(item != i && userItemMatrix(userID)(i) != -1) {
          println(itemSimilarityMatrix(item)(i))
          println(userItemMatrix(userID)(i))
          rating = rating + (itemSimilarityMatrix(item)(i) * userItemMatrix(userID)(i))
          totalSimilarity = totalSimilarity + itemSimilarityMatrix(item)(i)
        }
    }
    if(totalSimilarity != 0) {
      rating = rating / totalSimilarity
    }
    rating
  }

}
