package kr.kakaotech.community.global.monitoring;

public class QueryCountHolder {

    private static final ThreadLocal<Integer> count = ThreadLocal.withInitial(() -> 0);

    public static void increment() {
        count.set(count.get() + 1);
    }

    public static int getCount() {
        return count.get();
    }

    public static void reset() {
        count.remove();
    }
}
