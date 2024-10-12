package xyz.petnil.partitionedexecutor;

class PowerOfTwo {
    private PowerOfTwo() {
    }

    // Function to check if a number is a power of two
    public static boolean isPowerOfTwo(int n) {
        // Check if n is greater than 0 and (n & (n - 1)) == 0
        return n > 0 && (n & (n - 1)) == 0;
    }
}
