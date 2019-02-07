# Kamon LightStep

Implements a [Kamon](https://kamon.io) `SpanReporter` for [LightStep](https://lightstep.com/).

```scala
Kamon.addReporter(new LightStepReporter())
```

Example configuration:

```
kamon {
  environment {
    service = "MyService"
  }
  trace {
    sampler = always
  }
  lightstep {
    host = "collector-grpc.lightstep.com"
    port = 443
    tls = true
    access-token = "your-access-token"
    include-environment-tags = true
  }
}
```
