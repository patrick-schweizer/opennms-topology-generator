package org.opennms.topogen;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class RandomConnectedPairGeneratorTest {

    @Test
    void shouldRejectListsWithLessThan2Elements() {
        assertThrows(IllegalArgumentException.class, () -> new RandomConnectedPairGenerator<>(null));
        assertThrows(IllegalArgumentException.class, () -> new RandomConnectedPairGenerator<>(Collections.emptyList()));
    }


    @Test
    void shouldRejectListsWichContainsOnlyTheSameElement() {
        List<String> list = Arrays.asList("same", "same", "same");
        assertThrows(IllegalArgumentException.class, () -> new RandomConnectedPairGenerator<>(list));
    }

    @Test
    void shouldNotPairElementsWithItself() {
        List<String> list = Arrays.asList("1", "2", "3");
        RandomConnectedPairGenerator generator = new RandomConnectedPairGenerator<>(list);
        for (int i = 0; i < 10; i++) {
            Pair pair = generator.next();
            assertNotSame(pair.getLeft(), pair.getRight());
        }
    }

}
