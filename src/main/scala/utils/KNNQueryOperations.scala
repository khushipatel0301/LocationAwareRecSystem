package utils

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.lit
import play.api.libs.json.JsValue

object KNNQueryOperations {

  def performSpatialKNNQuery(sparkSession: SparkSession, latitude : JsValue, longitude : JsValue, dataFrame : DataFrame, k : Long) : DataFrame = {

    dataFrame.createOrReplaceTempView("input")

    val updatedDf : DataFrame = sparkSession.sql("select item, ST_PointFromText(item_location,',') as item_location from input").dropDuplicates()

    updatedDf.createOrReplaceTempView("updated_input")

    var finalInputDf : DataFrame = sparkSession.sql("SELECT item, item_location, ST_Distance(item_location, ST_Point(" + latitude + ", " + longitude + ")) as distance FROM updated_input ORDER BY distance LIMIT " +  k)

    finalInputDf.show()

    finalInputDf = finalInputDf.withColumn("travelPenalty", lit(0.0)).withColumn("recScore", lit(0.0))

    finalInputDf

  }
}
