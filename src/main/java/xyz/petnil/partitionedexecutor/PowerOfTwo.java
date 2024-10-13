package xyz.petnil.partitionedexecutor;

class PowerOfTwo {
    private PowerOfTwo() {
    }

    // Function to check if a number is a power of two
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
