package SpatialRatingsForNonSpatial

import org.apache.spark.sql.SparkSession

trait RecommendationFormat {

  def provideTopKRecommendation(sparkSession: SparkSession, k : Integer, maxPossibleRating : Double): Unit

}
