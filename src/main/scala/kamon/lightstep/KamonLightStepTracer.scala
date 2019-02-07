package com.lightstep.tracer.shared

import com.lightstep.tracer.jre.JRETracer
import com.lightstep.tracer.grpc.{Span => GrpcSpan}

class KamonLightStepTracer(options: Options) extends JRETracer(options) {
  def addGrpcSpan(span: GrpcSpan) = this.addSpan(span)
}
