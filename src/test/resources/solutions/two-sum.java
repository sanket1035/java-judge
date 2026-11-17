import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        long target = sc.nextLong();
        Map<Long, Integer> seen = new HashMap<>();
        for (int j = 0; j < n; j++) {
            long x = sc.nextLong();
            Integer i = seen.get(target - x);
            if (i != null) { System.out.println(i + " " + j); return; }
            seen.putIfAbsent(x, j);
        }
        System.out.println("-1 -1");
    }
}
