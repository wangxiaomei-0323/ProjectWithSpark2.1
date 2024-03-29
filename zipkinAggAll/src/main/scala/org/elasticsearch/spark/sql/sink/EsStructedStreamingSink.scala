package org.elasticsearch.spark.sql.sink

import java.util.{UUID}
import org.apache.commons.logging.{Log, LogFactory}
import org.apache.spark.TaskContext
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.execution.SQLExecution
import org.apache.spark.sql.execution.streaming.{MetadataLog, Sink}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.elasticsearch.hadoop.cfg.{ConfigurationOptions, Settings}
import org.elasticsearch.spark.sql.streaming._

/**
  * @author xiaomei.wang
  * @date 2019/6/22 15:43
  * @version 1.0
  */
class EsStructedStreamingSink(sparkSession: SparkSession, settings: Settings) extends Sink {
  @transient protected lazy val logger: Log = LogFactory.getLog(this.getClass)

  private val writeLog: MetadataLog[Array[EsSinkStatus]] = {
    if (SparkSqlStreamingConfigs.getSinkLogEnabled(settings)) {
      val logPath = SparkSqlStreamingConfigs.constructCommitLogPath(settings)
      logger.info(s"Using log path of [$logPath]")
      new EsSinkMetadataLog(settings, sparkSession, logPath)
    } else {
      logger.warn("EsSparkSqlStreamingSink is continuing without write commit log. " +
        "Be advised that data may be duplicated!")
      new NullMetadataLog[Array[EsSinkStatus]]()
    }
  }

  override def addBatch(batchId: Long, data: DataFrame): Unit = {
    if (batchId <= writeLog.getLatest().map(_._1).getOrElse(-1L)) {
      logger.info(s"Skipping already committed batch [$batchId]")
    } else {
      val commitProtocol = new EsCommitProtocol(writeLog)
      val queryExecution = data.queryExecution
      val schema = data.schema

      SQLExecution.withNewExecutionId(sparkSession, queryExecution) {
        val queryName = SparkSqlStreamingConfigs.getQueryName(settings).getOrElse(UUID.randomUUID().toString)
        val jobState = JobState(queryName, batchId)
        commitProtocol.initJob(jobState)
        try {
          val resource = settings.getProperty(ConfigurationOptions.ES_RESOURCE_WRITE)

          settings.setResourceWrite(resource)
          val serializedSettings = settings.save()
          val taskCommits = sparkSession.sparkContext.runJob(queryExecution.toRdd,
            (taskContext: TaskContext, iter: Iterator[InternalRow]) => {
              new EsStreamQueryWriter(serializedSettings, schema, commitProtocol).run(taskContext, iter)
            }
          )
          commitProtocol.commitJob(jobState, taskCommits)
        } catch {
          case t: Throwable =>
            commitProtocol.abortJob(jobState)
            throw t;
        }
      }
    }
  }
}
