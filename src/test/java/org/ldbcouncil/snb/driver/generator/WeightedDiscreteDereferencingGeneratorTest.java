package org.ldbcouncil.snb.driver.generator;

import org.ldbcouncil.snb.driver.util.Bucket.DiscreteBucket;
import org.ldbcouncil.snb.driver.util.Histogram;
import org.ldbcouncil.snb.driver.util.Tuple;
import org.ldbcouncil.snb.driver.util.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class WeightedDiscreteDereferencingGeneratorTest extends GeneratorTest<String, Integer> {

    @Override
    public Histogram<String, Integer> getExpectedDistribution() {
        Histogram<String, Integer> expectedDistribution = new Histogram<String, Integer>(0);
        expectedDistribution.addBucket(DiscreteBucket.create("1"), 1);
        expectedDistribution.addBucket(DiscreteBucket.create("2"), 2);
        expectedDistribution.addBucket(DiscreteBucket.create("3"), 4);
        expectedDistribution.addBucket(DiscreteBucket.create("4"), 8);
        return expectedDistribution;
    }

    @Override
    public double getDistributionTolerance() {
        return 0.01;
    }

    @Override
    public Iterator<String> getGeneratorImpl(GeneratorFactory generatorFactory) {
        List<Tuple2<Double, Iterator<String>>> items = new ArrayList<Tuple2<Double, Iterator<String>>>();
        items.add(Tuple.tuple2(1.0, generatorFactory.constant("1")));
        items.add(Tuple.tuple2(2.0, generatorFactory.constant("2")));
        items.add(Tuple.tuple2(4.0, generatorFactory.constant("3")));
        items.add(Tuple.tuple2(8.0, generatorFactory.constant("4")));

        return generatorFactory.weightedDiscreteDereferencing(items);
    }

    @Test
    public void emptyConstructorTest() {
        // Given
        GeneratorFactory generatorFactory = new GeneratorFactory(new RandomDataGeneratorFactory());
        List<Tuple2<Double, Iterator<String>>> emptyItems = new ArrayList<Tuple2<Double, Iterator<String>>>();

        // When
        assertThrows(GeneratorException.class, () -> {generatorFactory.weightedDiscreteDereferencing(emptyItems);},
            "Empty DiscreteGenerator should throw exception on next()"
        );
    }
}
