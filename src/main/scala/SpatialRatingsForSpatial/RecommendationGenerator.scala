package SpatialRatingsForSpatial

import SpatialRatingsForNonSpatial.RecommendationFormat
import org.apache.spark.sql.SparkSession

class RecommendationGenerator extends RecommendationFormat {

  override def provideTopKRecommendation(sparkSession: SparkSession, k : Integer, maxPossibleRating : Double): Unit = {

  }

}
