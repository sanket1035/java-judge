import java.util.*;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String s = sc.next();
        Deque<Character> stack = new ArrayDeque<>();
        boolean ok = true;
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else {
                char open = c == ')' ? '(' : c == ']' ? '[' : '{';
                if (stack.isEmpty() || stack.pop() != open) {
                    ok = false;
                    break;
                }
            }
        }
        System.out.println(ok && stack.isEmpty() ? "YES" : "NO");
    }
}
