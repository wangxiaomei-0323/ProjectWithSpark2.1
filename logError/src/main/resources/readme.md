线上 1m
{'spark.aispeech.read.kafka.brokers':'10.24.1.10:6667,10.24.1.21:6667,10.24.1.32:6667,10.24.1.43:6667',
'spark.aispeech.read.kafka.topcis':'ba-prod-trace-log',
'spark.aispeech.read.kafka.startOffsets':'latest',
'spark.aispeech.write.es.nodes':'10.24.1.44,10.24.1.23,10.24.1.24',
'spark.aispeech.write.es.port':'9200',
'spark.aispeech.write.es.type':'summary',
'spark.aispeech.write.es.index':'log_error_1m-',
'spark.aispeech.data.error.code':'500,400',
'spark.aispeech.data.agg.duration.time':'1 minutes',
'spark.aispeech.checkpoint':'/user/rsbj_ba_backend/logError/checkpoint/1m/',
'spark.aispeech.data.watermark.delay':'30 seconds',
'spark.aispeech.read.kafka.maxOffsetsPerTrigger':'1500000',
'spark.aispeech.trigger.time':'15 seconds',
'spark.memory.fraction':'0.8',
'spark.memory.storageFraction':'0.2',
'spark.memory.offHeap.enabled':'true',
'spark.memory.offHeap.size':'2048mb'}

测试
{'spark.aispeech.read.kafka.brokers':'10.12.6.57:6667,10.12.6.58:6667,10.12.6.59:6667',
'spark.aispeech.read.kafka.topcis':'ba-test-trace-log',
'spark.aispeech.read.kafka.startOffsets':'latest',
'spark.aispeech.write.es.nodes':'10.24.1.44,10.24.1.23,10.24.1.24',
'spark.aispeech.write.es.port':'9200',
'spark.aispeech.write.es.type':'summary',
'spark.aispeech.write.es.index':'log_error_1m-',
'spark.aispeech.data.error.code':'500,400',
'spark.aispeech.data.agg.duration.time':'1 minutes',
'spark.aispeech.checkpoint':'/user/rsbj_ba_backend/logError/checkpoint/1m/',
'spark.aispeech.data.watermark.delay':'5 minutes',
'spark.aispeech.read.kafka.maxOffsetsPerTrigger':'500000',
'spark.aispeech.trigger.time':'15 seconds',
'spark.memory.fraction':'0.8',
'spark.memory.storageFraction':'0.2',
'spark.memory.offHeap.enabled':'true',
'spark.memory.offHeap.size':'1024mb'}