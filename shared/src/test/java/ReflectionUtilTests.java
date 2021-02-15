import com.lauriethefish.betterportals.shared.util.ReflectionUtil;
import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;

// Tests to make sure that ReflectionUtil works fine
public class ReflectionUtilTests {
    private static class TestObject {
        private double testField = 5.0;

        private TestObject() {}
        private TestObject(double param) {
            testField = param;
        }

        private int testMethod(int testParamA, int testParamB) {
            assertEquals(testParamA, 5);
            assertEquals(testParamB, 7);
            return 44;
        }
    }

    // Used to check that we can get/run a private field/method from the superclass
    private static class TestDerived extends TestObject { }

    @Test
    public void testFieldGet() {
        TestObject obj = new TestObject();
        assertEquals(ReflectionUtil.getField(obj, "testField"), 5.0);

        TestObject derived = new TestDerived();
        assertEquals(ReflectionUtil.getField(derived, "testField"), 5.0);
    }

    @Test
    public void testMethodRun() {
        TestObject obj = new TestObject();
        Object result = ReflectionUtil.runMethod(obj, "testMethod", new Class[]{int.class, int.class}, 5, 7);
        assertEquals(result, 44);

        TestDerived derived = new TestDerived();
        result = ReflectionUtil.runMethod(derived, "testMethod", new Class[]{int.class, int.class}, 5, 7);
        assertEquals(result, 44);
    }

    @Test
    public void testObjectInstantiate() {
        TestObject obj = (TestObject) ReflectionUtil.newInstance(TestObject.class);

        TestObject withParam = (TestObject) ReflectionUtil.newInstance(TestObject.class, new Class[]{double.class}, 4.0d);
        assertEquals(withParam.testField, 4.0);
    }
}
