import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(), k = sc.nextInt() % n;
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[(i + k) % n] = sc.nextInt();
        StringJoiner out = new StringJoiner(" ");
        for (int x : a) out.add(String.valueOf(x));
        System.out.println(out);
    }
}
