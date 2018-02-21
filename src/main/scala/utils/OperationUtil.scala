package utils

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{min,max}

object OperationUtil {

  def giveTotalEntriesInDataFrame(df : DataFrame) : Long = {
    df.collect().length
  }

  def giveTotalUniqueNumber(df : DataFrame, columnName : String) : Integer = {
    df.select(columnName).dropDuplicates().collectAsList().size()
  }

  def getHighestDistance(sparkSession: SparkSession ,df : DataFrame) : Double = {

    val maxDistanceString : String = df.agg(max("distance")).head().toString()

    val maxDistance : Double = maxDistanceString.substring(1, maxDistanceString.length - 1).toDouble

    maxDistance
  }

  def getLowestRecScore(sparkSession: SparkSession ,df : DataFrame) : Double = {

    val minRecScoreString : String = df.agg(min("recScore")).head().toString()

    val lowestRecScore : Double = minRecScoreString.substring(1, minRecScoreString.length - 1).toDouble

    lowestRecScore

  }

}
