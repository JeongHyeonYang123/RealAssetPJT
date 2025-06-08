//package com.ssafy.home.scheduler;
//
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.Getter;
//import org.springframework.stereotype.Component;
//
//import java.time.ZoneId;
//import java.util.concurrent.*;
//
//@Component
//public class PostScheduler {
//
//    private final DelayQueue<DelayedPost> queue = new DelayQueue<>();
//    private final BoardService boardService;
//    private ExecutorService executor;
//
//    public PostScheduler(BoardService boardService) {
//        this.boardService = boardService;
//    }
//
//    /**
//     * 예약된 게시글을 큐에 추가합니다.
//     */
//    public void addPost(Board post) {
//        queue.put(new DelayedPost(post));
//    }
//
//    /**
//     * 애플리케이션 시작 시 스케줄러 스레드를 띄웁니다.
//     */
//    @PostConstruct
//    public void start() {
//        executor = Executors.newSingleThreadExecutor();
//        executor.submit(() -> {
//            while (!Thread.currentThread().isInterrupted()) {
//                try {
//                    DelayedPost delayed = queue.take();
//                    Board post = delayed.getPost();
//                    boardService.createPost(post);
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//    }
//
//    /**
//     * 애플리케이션 종료 시 스레드를 종료합니다.
//     */
//    @PreDestroy
//    public void shutdown() {
//        if (executor != null) {
//            executor.shutdownNow();
//        }
//    }
//
//    /**
//     * DelayQueue에서 사용하는 내부 클래스: 게시글, 예약 발행 시각 기반 딜레이
//     */
//    private static class DelayedPost implements Delayed {
//        @Getter
//        private final Board post;
//        private final long triggerTime;
//
//        public DelayedPost(Board post) {
//            this.post = post;
//            this.triggerTime = post.getCreatedAt()
//                    .atZone(ZoneId.systemDefault())
//                    .toInstant()
//                    .toEpochMilli();
//        }
//
//        @Override
//        public long getDelay(TimeUnit unit) {
//            long diff = triggerTime - System.currentTimeMillis();
//            return unit.convert(diff, TimeUnit.MILLISECONDS);
//        }
//
//        @Override
//        public int compareTo(Delayed o) {
//            if (o == this) return 0;
//            long d = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
//            return Long.compare(d, 0L);
//        }
//    }
//}
