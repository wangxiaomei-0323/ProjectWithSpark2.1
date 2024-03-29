package org.apache.spark

import org.apache.spark.aispeech.udf.{DurationConcat, SparkUserUDF}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * 按时间段做聚合
  *
  * @author xiaomei.wang
  * @date 2019/6/13 18:58
  * @version 1.0
  */
case class SpanSummary(spark: SparkSession) {

  import spark.implicits._

  val conf = spark.sparkContext.getConf

  def doAgg(df: DataFrame, aggTime: String) = {
    /* UDAF：多列合并成一个list */
    lazy val concatDur = new DurationConcat(",")
    // 元素之间的连接符
    val contactChar = "&"

    val computePercent = udf((durations: String, index: Int) => {
      if (durations.isEmpty) ""
      else {
        val source = durations.split(contactChar)
        val place = if (index >= source.length) source.length - 1 else if (index < 0) 0 else index
        source(place)
      }
    })

    val groupFromService = udf((messageString: String) => SparkUserUDF.collectFromSericeWithDuration(messageString.split(",").toList, contactChar))

    val res = df
      .withWatermark("logTime", conf.get("spark.aispeech.data.span.watermark.delay"))
      .groupBy(window($"logTime", aggTime, aggTime), $"serviceName", /*$"fromService", */ $"method", $"path")
      .agg(//approx_count_distinct($"id").as("fromService_query_amount"),
        concatDur(concat_ws(contactChar, $"id", $"fromService", $"duration")) as "messageList")
      .select($"window", $"serviceName", $"method", $"path",
        explode(split(groupFromService($"messageList"), ",")).as("fromServiceDurationList"))
      .withColumn("splitList", split($"fromServiceDurationList", ":"))
      .withColumn("fromService", $"splitList".getItem(0))
      .withColumn("durationList", $"splitList".getItem(1))
      .withColumn("query_amount", $"splitList".getItem(2))
      .withColumn("avg_duration", $"splitList".getItem(3))
      .select(md5(concat($"serviceName", $"fromService", $"method", $"path")).as("id"),
        $"window.start".cast("long").*(1000).as("beginTimestamp"),
        $"window.end".cast("long").*(1000).as("endTimestamp"),
        date_format($"window.start".cast("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS+08:00").as("beginTime"),
        date_format($"window.end".cast("timestamp"), "yyyy-MM-dd'T'HH:mm:ss.SSS+08:00").as("endTime"),
        $"serviceName",
        $"fromService",
        $"method",
        $"path",
        $"query_amount".cast("int"),
        $"avg_duration".cast("int"),
        computePercent($"durationList", $"query_amount".*(0).cast("int")).cast("int").as("min_duration"),
        computePercent($"durationList", $"query_amount".*(1).cast("int")).cast("int").as("max_duration"),
        computePercent($"durationList", $"query_amount".*(0.5).cast("int")).cast("int").as("p50_duration"),
        computePercent($"durationList", $"query_amount".*(0.9).cast("int")).cast("int").as("p90_duration"),
        computePercent($"durationList", $"query_amount".*(0.99).cast("int")).cast("int").as("p99_duration"))
    res.printSchema()
    res
  }

  /**
    * 将数据解析成需要的格式
    *
    * @param df
    * @return
    */
  def parseSpan(df: DataFrame) = {
    df.selectExpr("cast(value as String)")
      .withColumn("span", split(($"value"), ","))
      .select($"span".getItem(0).as("traceId"),
        $"span".getItem(1).as("id"),
        $"span".getItem(2).as("serviceName"),
        $"span".getItem(3).as("fromService"),
        $"span".getItem(4).as("duration"),
        $"span".getItem(5).as("path"),
        $"span".getItem(6).as("method"),
        $"span".getItem(7).as("logTime").cast("timestamp"))
      .filter(
        """
          | traceId is not null
          | and id is not null
          | and serviceName is not null
          | and fromService is not null
          | and duration is not null
          | and path is not null
          | and method is not null
          | and logTime is not null
        """.stripMargin)
  }
}
