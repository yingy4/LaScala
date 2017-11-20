/*
 * LaScala
 * Copyright (c) 2017. Phasmid Software
 */

package com.phasmid.laScala.equable;

import java.util.Iterator;

public class ComparableEquable extends Equable implements Comparable<ComparableEquable> {

    public ComparableEquable(Iterable<?> elements) {
        super(elements);
    }

    @Override
    public int compareTo(ComparableEquable o) {
        Iterator<?> thisIterator = elements.iterator();
        Iterator<?> thatIterator = o.elements.iterator();
        while (thisIterator.hasNext()) {
            if (thatIterator.hasNext()) {
                final Object next1 = thisIterator.next();
                final Object next2 = thatIterator.next();
                if (next1 instanceof Comparable) {
                    final Comparable comparable1 = (Comparable) next1;
                    //noinspection unchecked
                    int cf = comparable1.compareTo(next2);
                    if (cf != 0)
                        return cf;
                } else
                    throw new ComparableEquableException("ComparableEquable can only compare elements which are themselves Comparable");
            } else
                throw new ComparableEquableException("ComparableEquable can only compare Equables of the same length");
        }
        return 0;
    }

    public class ComparableEquableException extends RuntimeException {
        public ComparableEquableException(String s) {
            super(s);
        }
    }
}
