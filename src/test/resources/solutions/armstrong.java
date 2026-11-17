import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int digits = String.valueOf(n).length();
        int sum = 0;
        for (int x = n; x > 0; x /= 10) sum += (int) Math.pow(x % 10, digits);
        System.out.println(sum == n ? "YES" : "NO");
    }
}
