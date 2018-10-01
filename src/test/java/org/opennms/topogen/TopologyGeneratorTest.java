package org.opennms.topogen;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class TopologyGeneratorTest {
    @Test
    void shouldRejectUnknownTopology() throws IOException {
        TopologyGenerator generator = new TopologyGenerator(null);
        generator.setTopology("invalid topology");
        assertThrows(IllegalArgumentException.class, generator::assertSetup);
    }
}