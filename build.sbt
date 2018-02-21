import sbt.Keys.{libraryDependencies, version}

name := "LocationAwareRecommendationSystem"

version := "0.1"

scalaVersion := "2.11.8"

val SparkVersion = "2.1.0"

val GeoSparkVersion = "1.0.1"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5"
dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"

libraryDependencies ++= Seq(
  "org.scalaj" %% "scalaj-http" % "2.3.0",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "org.apache.spark" %% "spark-core" % SparkVersion % "provided" withSources(),
  "org.apache.spark" %% "spark-sql" % SparkVersion % "provided" withSources(),
  "org.apache.spark" %% "spark-mllib" % SparkVersion % "provided" withSources(),
  "org.datasyslab" % "geospark" % GeoSparkVersion,
  "org.datasyslab" % "geospark-sql" % GeoSparkVersion
)

resolvers +=
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools"
        