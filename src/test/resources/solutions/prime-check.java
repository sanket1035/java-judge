import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        long n = sc.nextLong();
        boolean prime = n >= 2;
        for (long d = 2; d * d <= n && prime; d++) {
            if (n % d == 0) prime = false;
        }
        System.out.println(prime ? "YES" : "NO");
    }
}
