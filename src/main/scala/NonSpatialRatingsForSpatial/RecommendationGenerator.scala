package NonSpatialRatingsForSpatial

import SpatialRatingsForNonSpatial.RecommendationFormat
import org.apache.spark.sql.{DataFrame, SQLContext, SparkSession}
import utils._
import org.apache.spark.sql.functions._

class RecommendationGenerator extends RecommendationFormat {

  override def provideTopKRecommendation(sparkSession: SparkSession, k : Integer, maxPossibleRating : Double): Unit = {

    val (latitude, longitude) = LocationUtil.getLocationCoordinates("")

    var df : DataFrame = sparkSession
      .read
      .format("csv")
      .option("header", true)
      .schema(Constants.NON_SPATIAL_RATINGS_FOR_SPATIAL_ITEMS_SCHEMA)
      .load("src/main/scala/data/SampleLARS.csv")

    val totalUniqueUsers : Integer = OperationUtil.giveTotalUniqueNumber(df, "userID")
    val totalUniqueItems : Integer = OperationUtil.giveTotalUniqueNumber(df, "item")

    val userIDs : Array[String] = df.select("userID").dropDuplicates().orderBy("userID").rdd.map(r => r(0).asInstanceOf[String]).collect()
    val items : Array[String] = df.select("item").dropDuplicates().orderBy("item").rdd.map(r => r(0).asInstanceOf[String]).collect()

    var userItemRatings = Array.ofDim[Double](totalUniqueUsers,totalUniqueItems)
    var itemCosineSimilarity = Array.ofDim[Double](totalUniqueItems, totalUniqueItems)

    userItemRatings = CosineSimilarityOperation.populateUserItemRatingMatrix(sparkSession, df, userItemRatings, userIDs, items)
    itemCosineSimilarity = CosineSimilarityOperation.createSimilarityMatrix(sparkSession, df, itemCosineSimilarity, userItemRatings, totalUniqueItems, totalUniqueUsers)

    val totalEntries : Long = OperationUtil.giveTotalEntriesInDataFrame(df)

    var itemDataFrame : DataFrame = KNNQueryOperations.performSpatialKNNQuery(sparkSession, latitude, longitude, df, totalEntries)

    val maxDistance : Double = OperationUtil.getHighestDistance(sparkSession, itemDataFrame)

    itemDataFrame = TravelPenaltyOperations.calculateTravelPenaltyForDataFrame(itemDataFrame, maxDistance, maxPossibleRating).orderBy("travelPenalty")

    itemDataFrame.show()

    itemDataFrame.createOrReplaceTempView("input")

    var tempRec : DataFrame = sparkSession.sql("SELECT *, row_number() over (order by (select null)) as rowID FROM input")

    tempRec.show()

    tempRec.createOrReplaceTempView("input")

    tempRec = sparkSession.sql("SELECT * FROM input WHERE rowID <= " + k)
    tempRec = tempRec.drop("rowID")

    tempRec.show()

    var recScoreDf : DataFrame = TravelPenaltyOperations.calculateRecScoreForDataFrame(tempRec, itemCosineSimilarity, userItemRatings, userIDs, totalUniqueItems)

    var recommendation : DataFrame = recScoreDf.orderBy(col("recScore").desc)

    val lowestRecScore : Double = OperationUtil.getLowestRecScore(sparkSession, recommendation)

    println(lowestRecScore)

    val finalRecommendations : DataFrame = TravelPenaltyOperations.calculateKRecommendationFromTravelPenaltyOfDataFrame(sparkSession, itemDataFrame, k, lowestRecScore, recommendation, maxPossibleRating, itemCosineSimilarity, userItemRatings, totalUniqueItems, userIDs)

    finalRecommendations.show()
  }

}
