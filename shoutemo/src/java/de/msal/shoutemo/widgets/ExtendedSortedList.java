/*
 * Copyright 2016 Maximilian Salomon.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package de.msal.shoutemo.widgets;

import android.support.v7.util.SortedList;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @since 11.02.16
 */
public class ExtendedSortedList<T> extends SortedList<T> {

    private int modCount = 0;

    public ExtendedSortedList(Class<T> klass, Callback<T> callback) {
        super(klass, callback);
    }

    public ExtendedSortedList(Class<T> klass, Callback<T> callback, int initialCapacity) {
        super(klass, callback, initialCapacity);
    }

    /**
     * Searches this {@code ExtendedSortedList} for the specified object.
     *
     * @param object the object to search for.
     * @return {@code true} if {@code object} is an element of this {@code ExtendedSortedList},
     * {@code false} otherwise
     */
    public boolean contains(Object object) {
        int s = size();
        if (object != null) {
            for (int i = 0; i < s; i++) {
                if (object.equals(get(i))) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < s; i++) {
                if (get(i) == null) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Removes all objects from this {@code Collection} that are not also found in the
     * {@code Collection} passed (optional). After this method returns this {@code Collection}
     * will only contain elements that also can be found in the {@code Collection}
     * passed to this method.
     * <p>
     * This implementation iterates over this {@code Collection} and tests for each
     * element {@code e} returned by the iterator, whether it is contained in
     * the specified {@code Collection}. If this test is negative, then the {@code
     * remove} method is called on the iterator. If the iterator does not
     * support removing elements, an {@code UnsupportedOperationException} is
     * thrown.
     *
     * @param collection
     *            the collection of objects to retain.
     * @return {@code true} if this {@code Collection} is modified, {@code false}
     *         otherwise.
     * @throws UnsupportedOperationException
     *                if removing from this {@code Collection} is not supported.
     * @throws ClassCastException
     *                if one or more elements of {@code collection}
     *                isn't of the correct type.
     * @throws NullPointerException
     *                if {@code collection} contains at least one
     *                {@code null} element and this {@code Collection} doesn't support
     *                {@code null} elements.
     * @throws NullPointerException
     *                if {@code collection} is {@code null}.
     */
    public boolean retainAll(Collection<?> collection) {
        boolean result = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (!collection.contains(it.next())) {
                it.remove();
                result = true;
            }
        }
        return result;
    }


    public Iterator<T> iterator() {
        return new ExtendedSortedListIterator();
    }

    private class ExtendedSortedListIterator implements Iterator<T> {

        /** Number of elements remaining in this iteration */
        private int remaining = size();

        /** Index of element that remove() would remove, or -1 if no such elt */
        private int removalIndex = -1;

        /** The expected modCount value */
        private int expectedModCount = modCount;

        public boolean hasNext() {
            return remaining != 0;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            ExtendedSortedList<T> ourList = ExtendedSortedList.this;
            int rem = remaining;
            if (ourList.modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (rem == 0) {
                throw new NoSuchElementException();
            }
            remaining = rem - 1;
            return ourList.get(removalIndex = ourList.size() - rem);
        }

        public void remove() {
            int removalIdx = removalIndex;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (removalIdx < 0) {
                throw new IllegalStateException();
            }
            removeItemAt(removalIdx);
            removalIndex = -1;
            expectedModCount = ++modCount;
        }
    }

}
