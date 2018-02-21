package utils

import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

object Constants {

  val NON_SPATIAL_RATINGS_FOR_SPATIAL_ITEMS_SCHEMA = StructType(
    Array(
      StructField("userID", StringType),
      StructField("item", StringType),
      StructField("item_location", StringType),
      StructField("ratings", DoubleType)
    )
  )

}
