package com.vyazelenko.d4ll;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class BadExample
{
    private long nextId = System.currentTimeMillis();

    @Benchmark
    public Long wrapper()
    {
        return ++nextId;
    }

    @Benchmark
    public long primitive()
    {
        return ++nextId;
    }
}
