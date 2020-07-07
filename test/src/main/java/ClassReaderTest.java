import jdk.internal.org.objectweb.asm.ClassReader;

public class ClassReaderTest {

    public static void main(String[] args) throws Exception {

        ClassReader classReader = new ClassReader(C.class.getProtectionDomain().getCodeSource().getLocation().openStream());
        System.out.println(classReader.getClassName());
    }

}
