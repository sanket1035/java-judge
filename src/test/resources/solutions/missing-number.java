import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        long n = sc.nextLong();
        long sum = 0;
        for (long i = 0; i < n - 1; i++) sum += sc.nextLong();
        System.out.println(n * (n + 1) / 2 - sum);
    }
}
