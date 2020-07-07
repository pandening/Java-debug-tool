import java.util.concurrent.TimeUnit;

public class BlockMain {

    public static void main(String[] args) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                for(;;) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }
                    System.out.println("1 sec");
                }
            }
        }, "test").start();

    }

}
