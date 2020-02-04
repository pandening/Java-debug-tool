import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Demo {

    private void say(String word, boolean tag, int rdm) {
        if (word == null) {
            word = "test say";
        }
        int length = word.length();
        if (tag) {
            length += 1;
        } else {
            length -= 1;
        }
        word += "@" + length;
        System.out.println(word);
        if (rdm > 5) {
            throw new IllegalStateException("test exception");
        }
    }

    private static final String[] list = {"a", "ab", "abc", "abcd"};

    public static void main(String[] args) {
        Demo demo = new Demo();
        Random random = new Random(47);
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        demo.say(list[random.nextInt(4)], random.nextBoolean(), random.nextInt(10));
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "demo-thread").start();
    }


}
