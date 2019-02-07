package kamon.lightstep

import java.nio.ByteBuffer

import com.google.protobuf.Timestamp
import com.lightstep.tracer.grpc.Reference.Relationship
import com.lightstep.tracer.shared.Options.OptionsBuilder
import com.typesafe.config.Config
import kamon.{Kamon, SpanReporter}
import kamon.trace.Span.{Mark, TagValue, FinishedSpan => KamonSpan}
import com.lightstep.tracer.grpc.{KeyValue, Log, Reference, Span => LightStepSpan}
import com.lightstep.tracer.jre.JRETracer
import com.lightstep.tracer.shared.{KamonLightStepTracer, SpanContext => LightStepSpanContext}
import io.opentracing.Span
import kamon.trace.IdentityProvider.Identifier
import kamon.util.Clock

import scala.util.Try

class LightStepReporter extends SpanReporter {
  @volatile private var lightStepClient: LightStepClient = _
  reconfigure(Kamon.config())

  override def reconfigure(newConfig: Config): Unit = {
    val lightStepConfig = newConfig.getConfig("kamon.lightstep")
    val accessToken = lightStepConfig.getString("access-token")
    val host = Option(lightStepConfig.getString("host"))
    val port = Option(lightStepConfig.getInt("port"))
    val scheme = if (lightStepConfig.getBoolean("tls")) "https" else "http"
    val includeEnvironmentTags =
      lightStepConfig.getBoolean("include-environment-tags")

    lightStepClient = new LightStepClient(accessToken, host, port, scheme, includeEnvironmentTags)
  }

  override def start(): Unit = {}
  override def stop(): Unit = {}

  override def reportSpans(spans: Seq[KamonSpan]): Unit = {
    lightStepClient.sendSpans(spans)
  }
}

class LightStepClient(accessToken: String,
                      host: Option[String],
                      port: Option[Int],
                      scheme: String,
                      includeEnvironmentTags: Boolean) {
  private val tracer = {
    val options = new OptionsBuilder()
      .withAccessToken(accessToken)
      .withCollectorProtocol(scheme)
      .withComponentName(Kamon.environment.service)

    host.foreach(options.withCollectorHost)
    port.foreach(options.withCollectorPort)


    if (includeEnvironmentTags) {
      Kamon.environment.tags
        .foreach { case (tag, value) => options.withTag(tag, value) }
    }

    new KamonLightStepTracer(options.build())
  }

  def sendSpans(spans: Seq[KamonSpan]): Unit = {
    spans.foreach(span => tracer.addGrpcSpan(convertSpan(span)))
    tracer.asInstanceOf[JRETracer].flush(5000)
  }

  private def convertTag(key: String, tagValue: TagValue) = {
    val keyValueBuilder = KeyValue.newBuilder().setKey(key)
    tagValue match {
      case TagValue.True => keyValueBuilder.setBoolValue(true)
      case TagValue.False => keyValueBuilder.setBoolValue(false)
      case TagValue.String(string) => keyValueBuilder.setStringValue(string)
      case TagValue.Number(number) => keyValueBuilder.setDoubleValue(number)
    }
    keyValueBuilder.build()
  }

  private def convertMark(mark: Mark) = {
    val logBuilder = Log.newBuilder().setTimestamp(
      Timestamp.newBuilder()
        .setSeconds(mark.instant.getEpochSecond)
        .setNanos(mark.instant.getNano))
    logBuilder.addFields(
      KeyValue.newBuilder()
        .setKey("event")
        .setStringValue(mark.key))
  }

  private def convertSpan(kamonSpan: KamonSpan): LightStepSpan = {
    val duration =
      Math.floorDiv(Clock.nanosBetween(kamonSpan.from, kamonSpan.to), 1000)
    val kamonContext = kamonSpan.context
    val span = LightStepSpan.newBuilder()
      .setStartTimestamp(Timestamp.newBuilder()
        .setSeconds(kamonSpan.from.getEpochSecond)
        .setNanos(kamonSpan.from.getNano))
      .setDurationMicros(duration)
      .setOperationName(kamonSpan.operationName)
      .setSpanContext(com.lightstep.tracer.grpc.SpanContext.newBuilder()
        .setSpanId(convertIdentifier(kamonSpan.context.spanID))
        .setTraceId(convertIdentifier(kamonSpan.context.traceID)))

    kamonSpan.tags.foreach { case (key, value) => span.addTags(convertTag(key, value)) }
    kamonSpan.marks.foreach(mark => span.addLogs(convertMark(mark)))

    if (kamonContext.parentID != null) {
      val refBuilder = Reference.newBuilder()
        .setSpanContext(
          new LightStepSpanContext(
            convertIdentifier(kamonContext.traceID), convertIdentifier(kamonContext.parentID)
          ).getInnerSpanCtx)
      refBuilder.setRelationship(Relationship.CHILD_OF)
      span.addReferences(refBuilder)
    }

    span.build()
  }

  private def convertIdentifier(identifier: Identifier): Long = {
    Try {
      // Assumes that Kamon was configured to use the default identity generator.
      ByteBuffer.wrap(identifier.bytes).getLong
    }.getOrElse(0L)
  }
}
