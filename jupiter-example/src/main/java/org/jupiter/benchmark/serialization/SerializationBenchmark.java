package org.jupiter.benchmark.serialization;

import io.netty.buffer.*;
import io.netty.util.internal.PlatformDependent;
import org.jupiter.common.util.Lists;
import org.jupiter.serialization.*;
import org.jupiter.serialization.io.InputBuf;
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.transport.netty.alloc.AdaptiveOutputBufAllocator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SerializationBenchmark {

    /*
        Benchmark                                     Mode  Cnt    Score    Error   Units
        SerializationBenchmark.hessianByteBuffer     thrpt   10  150.256 ±  8.589  ops/ms
        SerializationBenchmark.hessianBytesArray     thrpt   10  152.689 ±  2.453  ops/ms
        SerializationBenchmark.javaByteBuffer        thrpt   10   25.228 ±  1.723  ops/ms
        SerializationBenchmark.javaBytesArray        thrpt   10   25.912 ±  1.083  ops/ms
        SerializationBenchmark.kryoByteBuffer        thrpt   10  303.410 ± 10.243  ops/ms
        SerializationBenchmark.kryoBytesArray        thrpt   10  443.201 ±  8.727  ops/ms
        SerializationBenchmark.protoStuffByteBuffer  thrpt   10  799.531 ± 16.915  ops/ms
        SerializationBenchmark.protoStuffBytesArray  thrpt   10  687.980 ± 18.072  ops/ms
        SerializationBenchmark.hessianByteBuffer      avgt   10    0.007 ±  0.001   ms/op
        SerializationBenchmark.hessianBytesArray      avgt   10    0.007 ±  0.001   ms/op
        SerializationBenchmark.javaByteBuffer         avgt   10    0.040 ±  0.011   ms/op
        SerializationBenchmark.javaBytesArray         avgt   10    0.038 ±  0.001   ms/op
        SerializationBenchmark.kryoByteBuffer         avgt   10    0.003 ±  0.001   ms/op
        SerializationBenchmark.kryoBytesArray         avgt   10    0.002 ±  0.001   ms/op
        SerializationBenchmark.protoStuffByteBuffer   avgt   10    0.001 ±  0.001   ms/op
        SerializationBenchmark.protoStuffBytesArray   avgt   10    0.001 ±  0.001   ms/op
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final Serializer javaSerializer = SerializerFactory.getSerializer(SerializerType.JAVA.value());
    private static final Serializer hessianSerializer = SerializerFactory.getSerializer(SerializerType.HESSIAN.value());
    private static final Serializer protoStuffSerializer = SerializerFactory.getSerializer(SerializerType.PROTO_STUFF.value());
    private static final Serializer kryoSerializer = SerializerFactory.getSerializer(SerializerType.KRYO.value());

    private static final AdaptiveOutputBufAllocator.Handle allocHandle = AdaptiveOutputBufAllocator.DEFAULT.newHandle();
    private static final ByteBufAllocator allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

    static int USER_COUNT = 1;

    @Benchmark
    public void javaBytesArray() {
        byte[] bytes = javaSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        byteBuf.release();
        javaSerializer.readObject(bytes, Users.class);
    }

    @Benchmark
    public void javaByteBuffer() {
        OutputBuf outputBuf = javaSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        javaSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void hessianBytesArray() {
        byte[] bytes = hessianSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        byteBuf.release();
        hessianSerializer.readObject(bytes, Users.class);
    }

    @Benchmark
    public void hessianByteBuffer() {
        OutputBuf outputBuf = hessianSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        hessianSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void protoStuffBytesArray() {
        byte[] bytes = protoStuffSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        byteBuf.release();
        protoStuffSerializer.readObject(bytes, Users.class);
    }

    @Benchmark
    public void protoStuffByteBuffer() {
        OutputBuf outputBuf = protoStuffSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        protoStuffSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void kryoBytesArray() {
        byte[] bytes = kryoSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);
        byteBuf.release();
        kryoSerializer.readObject(bytes, Users.class);
    }

    @Benchmark
    public void kryoByteBuffer() {
        OutputBuf outputBuf = kryoSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        kryoSerializer.readObject(inputBuf, Users.class);
    }

    static final class NettyInputBuf implements InputBuf {

        private final ByteBuf byteBuf;

        NettyInputBuf(ByteBuf byteBuf) {
            this.byteBuf = byteBuf;
        }

        @Override
        public InputStream inputStream() {
            return new ByteBufInputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer() {
            return byteBuf.nioBuffer(); // should not be called more than once
        }

        @Override
        public int size() {
            return byteBuf.readableBytes();
        }

        @Override
        public boolean hasMemoryAddress() {
            return byteBuf.hasMemoryAddress();
        }

        @Override
        public boolean release() {
            return byteBuf.release();
        }
    }

    static final class NettyOutputBuf implements OutputBuf {

        private final AdaptiveOutputBufAllocator.Handle allocHandle;
        private final ByteBuf byteBuf;
        private ByteBuffer nioByteBuffer;

        public NettyOutputBuf(AdaptiveOutputBufAllocator.Handle allocHandle, ByteBufAllocator alloc) {
            this.allocHandle = allocHandle;
            byteBuf = allocHandle.allocate(alloc);
        }

        @Override
        public OutputStream outputStream() {
            return new ByteBufOutputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer(int minWritableBytes) {
            if (minWritableBytes < 0) {
                minWritableBytes = byteBuf.writableBytes();
            }

            if (nioByteBuffer == null) {
                nioByteBuffer = newNioByteBuffer(byteBuf, minWritableBytes);
            }

            if (nioByteBuffer.remaining() >= minWritableBytes) {
                return nioByteBuffer;
            }

            int position = nioByteBuffer.position();

            nioByteBuffer = newNioByteBuffer(byteBuf, position + minWritableBytes);

            nioByteBuffer.position(position);

            return nioByteBuffer;
        }

        @Override
        public int size() {
            if (nioByteBuffer == null) {
                return byteBuf.readableBytes();
            }
            return Math.max(byteBuf.readableBytes(), nioByteBuffer.position());
        }

        @Override
        public boolean hasMemoryAddress() {
            return byteBuf.hasMemoryAddress();
        }

        @Override
        public Object backingObject() {
            int actualWriteBytes = byteBuf.writerIndex();
            if (nioByteBuffer != null) {
                actualWriteBytes += nioByteBuffer.position();
            }

            allocHandle.record(actualWriteBytes);

            return byteBuf.writerIndex(actualWriteBytes);
        }

        private static ByteBuffer newNioByteBuffer(ByteBuf byteBuf, int writableBytes) {
            return byteBuf
                    .ensureWritable(writableBytes)
                    .nioBuffer(byteBuf.writerIndex(), byteBuf.writableBytes());
        }
    }

    static Users createUsers(int count) {
        List<User> userList = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            userList.add(createUser());
        }
        Users users = new Users();
        users.setUsers(userList);
        return users;
    }

    static User createUser() {
        User user = new User();
        user.setId(ThreadLocalRandom.current().nextInt());
        user.setName("block");
        user.setSex(0);
        user.setBirthday(new Date());
        user.setEmail("xxx@alibaba-inc.con");
        user.setMobile("18325038521");
        user.setAddress("浙江省 杭州市 文一西路969号");
        List<Integer> permsList = Lists.newArrayList(1, 12, 123);
        user.setPermissions(permsList);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }

    static class Users implements Serializable {

        private List<User> users;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }

    static class User implements Serializable {

        private long id;
        private String name;
        private int sex;
        private Date birthday;
        private String email;
        private String mobile;
        private String address;
        private List<Integer> permissions;
        private int status;
        private Date createTime;
        private Date updateTime;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public List<Integer> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<Integer> permissions) {
            this.permissions = permissions;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", sex=" + sex +
                    ", birthday=" + birthday +
                    ", email='" + email + '\'' +
                    ", mobile='" + mobile + '\'' +
                    ", address='" + address + '\'' +
                    ", permissions=" + permissions +
                    ", status=" + status +
                    ", createTime=" + createTime +
                    ", updateTime=" + updateTime +
                    '}';
        }
    }
}
