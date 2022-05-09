package meta.works.zim.annotationhelper.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Used to "remember" an arbitrary number of items, but after N (or so) insertions, things start to
 * get a little bit fuzzy. Used for duplicate suppression of data from infinite streams.
 */
public
class LossySet<T> implements Set<T>
{
    final int magnitude;

    int size = 0;

    private
    Set<T> currentTarget = new HashSet<>();

    private
    Set<T> previousTarget = new HashSet<>();

    public
    LossySet(final int magnitude)
    {
        this.magnitude = magnitude;
    }

    @Override
    public
    int size()
    {
        return size;
    }

    @Override
    public
    boolean isEmpty()
    {
        return size != 0;
    }

    @Override
    public
    boolean contains(final Object o)
    {
        return currentTarget.contains(o) || previousTarget.contains(o);
    }

    @Override
    public
    Iterator<T> iterator()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    Object[] toArray()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    <T1> T1[] toArray(final T1[] t1s)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    boolean add(final T t)
    {
        if (currentTarget.size() > magnitude)
        {
            // ROTATE!
            previousTarget = currentTarget;
            currentTarget = new HashSet<>();
        }

        if (previousTarget.remove(t))
        {
            currentTarget.add(t);
            return false;
        }

        return currentTarget.add(t);
    }

    @Override
    public
    boolean remove(final Object o)
    {
        return currentTarget.remove(o) || previousTarget.remove(o);
    }

    @Override
    public
    boolean containsAll(final Collection<?> collection)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    boolean addAll(final Collection<? extends T> collection)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    boolean retainAll(final Collection<?> collection)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    boolean removeAll(final Collection<?> collection)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public
    void clear()
    {
        currentTarget.clear();
        previousTarget.clear();
    }
}
