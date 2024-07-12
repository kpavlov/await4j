package me.kpavlov.await4j;

class TestUtils {

    @SafeVarargs
    public static <T, U> Object[][] combine(T[] list1, U... list2) {
        final int I = list1.length;
        final int J = list2.length;
        final var result = new Object[I * J][2];
        for (int i = 0; i < I; i++) {
            for (int j = 0; j < J; j++) {
                result[i * J + j][0] = list1[i];
                result[i * J + j][1] = list2[j];
            }
        }
        return result;
    }
}
