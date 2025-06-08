//package com.ssafy.home.scheduler;
//
//import lombok.Getter;
//
//import java.time.ZoneId;
//import java.util.concurrent.Delayed;
//import java.util.concurrent.TimeUnit;
//
//public class DelayedPost implements Delayed {
//    @Getter
//    private final Board post;
//    private final long startTimeMillis;
//
//    public DelayedPost(Board post) {
//        this.post = post;
//        this.startTimeMillis = post.getCreatedAt()
//                .atZone(ZoneId.systemDefault())
//                .toInstant()
//                .toEpochMilli();
//    }
//
//    @Override
//    public long getDelay(TimeUnit unit) {
//        long delay = startTimeMillis - System.currentTimeMillis();
//        return unit.convert(delay, TimeUnit.MILLISECONDS);
//    }
//
//    @Override
//    public int compareTo(Delayed other) {
//        return Long.compare(
//                this.getDelay(TimeUnit.MILLISECONDS),
//                other.getDelay(TimeUnit.MILLISECONDS)
//        );
//    }
//}
