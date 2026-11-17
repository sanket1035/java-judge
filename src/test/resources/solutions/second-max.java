import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long first = Long.MIN_VALUE, second = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            long x = sc.nextLong();
            if (x > first) { second = first; first = x; }
            else if (x < first && x > second) { second = x; }
        }
        System.out.println(second == Long.MIN_VALUE ? -1 : second);
    }
}
