package xyz.petnil.partitionedexecutor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PowerOfTwoTest {

    @Test
    void shouldReturnTrueForPowerOfTwoNumbers() {
        // Given / When / Then
        assertThat(PowerOfTwo.isPowerOfTwo(1)).isTrue(); // 2^0 = 1
        assertThat(PowerOfTwo.isPowerOfTwo(2)).isTrue(); // 2^1 = 2
        assertThat(PowerOfTwo.isPowerOfTwo(4)).isTrue(); // 2^2 = 4
        assertThat(PowerOfTwo.isPowerOfTwo(8)).isTrue(); // 2^3 = 8
        assertThat(PowerOfTwo.isPowerOfTwo(16)).isTrue(); // 2^4 = 16
        assertThat(PowerOfTwo.isPowerOfTwo(1024)).isTrue(); // 2^10 = 1024
    }

    @Test
    void shouldReturnFalseForNonPowerOfTwoNumbers() {
        // Given / When / Then
        assertThat(PowerOfTwo.isPowerOfTwo(0)).isFalse(); // Zero is not a power of two
        assertThat(PowerOfTwo.isPowerOfTwo(3)).isFalse(); // 3 is not a power of two
        assertThat(PowerOfTwo.isPowerOfTwo(5)).isFalse(); // 5 is not a power of two
        assertThat(PowerOfTwo.isPowerOfTwo(6)).isFalse(); // 6 is not a power of two
        assertThat(PowerOfTwo.isPowerOfTwo(12)).isFalse(); // 12 is not a power of two
        assertThat(PowerOfTwo.isPowerOfTwo(1023)).isFalse(); // 1023 is not a power of two
    }

    @Test
    void shouldReturnFalseForNegativeNumbers() {
        // Given / When / Then
        assertThat(PowerOfTwo.isPowerOfTwo(-1)).isFalse(); // Negative number
        assertThat(PowerOfTwo.isPowerOfTwo(-2)).isFalse(); // Negative power of two
        assertThat(PowerOfTwo.isPowerOfTwo(-1024)).isFalse(); // Negative power of two
    }
}
