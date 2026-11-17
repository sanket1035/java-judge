import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        Set<Integer> seen = new LinkedHashSet<>();
        for (int i = 0; i < n; i++) seen.add(sc.nextInt());
        StringJoiner out = new StringJoiner(" ");
        for (int x : seen) out.add(String.valueOf(x));
        System.out.println(out);
    }
}
