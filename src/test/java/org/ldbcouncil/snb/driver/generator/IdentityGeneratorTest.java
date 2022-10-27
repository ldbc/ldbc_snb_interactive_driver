package org.ldbcouncil.snb.driver.generator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IdentityGeneratorTest {
    @Test
    public void shouldStopAtLimitTest() {
        // Given
        Integer[] numbers = new Integer[]{1, 2, 3, 4, 5, 6};
        Generator<Integer> identityGenerator = new IdentityGenerator<Integer>(numbers);

        // When
        List<Integer> generatorNumbers = new ArrayList<Integer>();
        while (identityGenerator.hasNext()) {
            generatorNumbers.add(identityGenerator.next());
        }

        // Then
        assertEquals(Arrays.asList(numbers), generatorNumbers);
    }
};
