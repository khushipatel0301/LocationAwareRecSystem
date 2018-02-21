package scala

import NonSpatialRatingsForSpatial.RecommendationGenerator
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql._
import org.datasyslab.geospark.serde.GeoSparkKryoRegistrator
import org.datasyslab.geosparksql.utils.GeoSparkSQLRegistrator

object LARS {

  def main(args: Array[String]): Unit = {

    val k = 4
    val maxPossibleRating : Double = 5.0

    val sparkConf : SparkConf = new SparkConf().setMaster("local[*]").setAppName(getClass.getSimpleName)

    val sc : SparkContext = new SparkContext(sparkConf)

    val sparkSession : SparkSession = SparkSession.builder
      .config(conf = sparkConf)
      .config("spark.serializer", classOf[KryoSerializer].getName)
      .config("spark.kryo.registrator", classOf[GeoSparkKryoRegistrator].getName)
      .config("spark.network.timeout", "600s")
      .config("spark.executor.heartbeatInterval", "100s")
      .appName(getClass.getSimpleName)
      .getOrCreate()

    GeoSparkSQLRegistrator.registerAll(sparkSession.sqlContext)

    val nonSpatialRatingsForSpatial = new RecommendationGenerator()
    nonSpatialRatingsForSpatial.provideTopKRecommendation(sparkSession, k, maxPossibleRating)

    sparkSession.stop()
    sc.stop()

  }
}
