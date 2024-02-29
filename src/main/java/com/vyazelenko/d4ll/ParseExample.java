package com.vyazelenko.d4ll;

import org.agrona.collections.Hashing;
import org.agrona.collections.Long2LongCounterMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Fork(3)
@Warmup(iterations = 10, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
public class ParseExample
{
    private static final int SIZE = 1024 * 1024;

    private final HashMap<Long, Long> hashMap = new HashMap<>(SIZE);
    private final Long2LongCounterMap counterMap = new Long2LongCounterMap(SIZE, Hashing.DEFAULT_LOAD_FACTOR, -1);
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(SIZE * 40);
    private final UnsafeBuffer agrona = new UnsafeBuffer();
    private final int[] lengths = new int[SIZE];
    private final int[] offsets = new int[SIZE];
    private int next;

    @Setup
    public void setup()
    {
        agrona.wrap(byteBuffer);
        final Random r = new Random(-4623848);
        int offset = 0;
        for (int i = 0; i < SIZE; i++)
        {
            final long value = r.nextLong(1_000_000, 1_000_000_000_000L);
            offsets[i] = offset;
            int written = agrona.putStringWithoutLengthAscii(offset, "{id:");
            written += agrona.putLongAscii(offset + written, value);
            written += agrona.putStringWithoutLengthAscii(offset + written, ",value:");
            written += agrona.putNaturalIntAscii(offset + written, r.nextInt(10, 100));
            written += agrona.putStringWithoutLengthAscii(offset + written, "}");
            lengths[i] = written;
            offset += written;
        }
    }

    @Benchmark
    public long jdk()
    {
        final int index = (next++ & (SIZE - 1));
        final int offset = offsets[index];
        final int length = lengths[index];

        final byte[] bytes = new byte[length];
        byteBuffer.position(offset).limit(offset + length).get(bytes);

        final String json = new String(bytes, StandardCharsets.US_ASCII);

        final long id = Long.parseLong(json, 4, length - 10, 10);
        final int value = Integer.parseInt(json, length - 3, length - 1, 10);

        final Long oldValue = hashMap.get(id);
        hashMap.put(id, null != oldValue ? oldValue + value : (long)value);
        return null != oldValue ? oldValue : 0;
    }

    @Benchmark
    public long agrona()
    {
        final int index = (next++ & (SIZE - 1));
        final int offset = offsets[index];
        final int length = lengths[index];

        agrona.wrap(byteBuffer, offset, length);

        final long id = agrona.parseNaturalLongAscii(4, length - 14);
        final int value = agrona.parseNaturalIntAscii(length - 3, 2);

        return counterMap.getAndAdd(id, value);
    }
}
