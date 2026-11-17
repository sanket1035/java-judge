import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long best = Long.MIN_VALUE, current = 0;
        for (int i = 0; i < n; i++) {
            long x = sc.nextLong();
            current = Math.max(x, current + x);
            best = Math.max(best, current);
        }
        System.out.println(best);
    }
}
