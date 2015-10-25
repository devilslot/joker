package cs.bilkent.zanza.operator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * For internal use
 * <p>
 * A utility interface that provides some methods for key-value accesses
 *
 * @param <K>
 *         type of the key
 */
interface Fields<K>
{
    <T> T get ( K key );

    boolean contains ( K key );

    void set ( K key, Object value );

    <T> T put ( K key, T value );

    Object remove ( K key );

    boolean delete ( K key );

    void clear ();

    int size ();

    /**
     * Returns immutable collection of keys
     *
     * @return immutable collection of keys
     */
    Collection<K> keys ();

    default <T> T getOrFail ( K key )
    {
        T val = get( key );
        if ( val != null )
        {
            return val;
        }

        throw new IllegalArgumentException( key + " not provided!" );
    }

    default <T> T getOrFail ( K key, Function<K, RuntimeException> exceptionFunc )
    {
        T val = get( key );
        if ( val != null )
        {
            return val;
        }

        throw exceptionFunc.apply( key );
    }

    default <T> T getOrDefault ( K key, T defaultVal )
    {
        T val = get( key );
        return val != null ? val : defaultVal;
    }

    default <T> T getOrDefault ( K key, Supplier<T> supplier )
    {
        T val = get( key );
        return val != null ? val : supplier.get();
    }

    default Object getObject ( final K key )
    {
        return get( key );
    }

    default Object getObjectOrDefault ( final K key, final Object defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default String getString ( final K key )
    {
        return get( key );
    }

    default String getStringOrDefault ( final K key, final String defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default Integer getInteger ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Integer ? (Integer) number : number.intValue();
        }

        return null;
    }

    default Integer getIntegerOrDefault ( final K key, final Integer defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Integer ? (Integer) number : number.intValue();
        }

        return defaultVal;
    }

    default int getIntegerValueOrDefault ( final K key, final int defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.intValue() : defaultVal;
    }

    default Long getLong ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Long ? (Long) number : number.longValue();
        }

        return null;
    }

    default Long getLongOrDefault ( final K key, final Long defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Long ? (Long) number : number.longValue();
        }

        return defaultVal;
    }

    default long getLongValueOrDefault ( final K key, final long defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.longValue() : defaultVal;
    }

    default Boolean getBoolean ( final K key )
    {
        return get( key );
    }

    default Boolean getBooleanOrDefault ( final K key, final Boolean defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default Short getShort ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Short ? (Short) number : number.shortValue();
        }

        return null;
    }

    default Short getShortOrDefault ( final K key, final Short defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Short ? (Short) number : number.shortValue();
        }

        return defaultVal;
    }

    default short getShortValueOrDefault ( final K key, final short defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.shortValue() : defaultVal;
    }

    default Byte getByte ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Byte ? (Byte) number : number.byteValue();
        }

        return null;
    }

    default Byte getByteOrDefault ( final K key, final Byte defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Byte ? (Byte) number : number.byteValue();
        }

        return defaultVal;
    }

    default byte getByteValueOrDefault ( final K key, final byte defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.byteValue() : defaultVal;
    }

    default Double getDouble ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Double ? (Double) number : number.doubleValue();
        }

        return null;
    }

    default Double getDoubleOrDefault ( final K key, final Double defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Double ? (Double) number : number.doubleValue();
        }

        return defaultVal;
    }

    default double getDoubleValueOrDefault ( final K key, final double defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.doubleValue() : defaultVal;
    }

    default Float getFloat ( final K key )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Float ? (Float) number : number.floatValue();
        }

        return null;
    }

    default Float getFloatOrDefault ( final K key, final Float defaultVal )
    {
        final Number number = get( key );
        if ( number != null )
        {
            return number instanceof Float ? (Float) number : number.floatValue();
        }

        return defaultVal;
    }

    default float getFloatValueOrDefault ( final K key, final float defaultVal )
    {
        final Number number = get( key );
        return number != null ? number.floatValue() : defaultVal;
    }

    default byte[] getBinary ( final K key )
    {
        return get( key );
    }

    default byte[] getBinaryOrDefault ( final K key, final byte[] defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default <T> Collection<T> getCollection ( final K key )
    {
        return get( key );
    }

    default <T> Collection<T> getCollectionOrDefault ( final K key, final Collection<T> defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default <T> Collection<T> getCollectionOrEmpty ( final K key )
    {
        return getOrDefault( key, Collections.emptyList() );
    }

    default <T> List<T> getList ( final K key )
    {
        return get( key );
    }

    default <T> List<T> getListOrDefault ( final K key, final List<T> defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default <T> List<T> getListOrEmpty ( final K key )
    {
        final List<T> list = getList( key );
        return list != null ? list : new ArrayList<>();
    }

    default <T> Set<T> getSet ( final K key )
    {
        return get( key );
    }

    default <T> Set<T> getSetOrDefault ( final K key, final Set<T> defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default <T> Set<T> getSetOrEmpty ( final K key )
    {
        final Set<T> set = getSet( key );
        return set != null ? set : new HashSet<>();
    }

    default <K2, V> Map<K2, V> getMap ( final K key )
    {
        return get( key );
    }

    default <K2, V> Map<K2, V> getMapOrDefault ( final K key, final Map<K2, V> defaultVal )
    {
        return getOrDefault( key, defaultVal );
    }

    default <K2, V> Map<K2, V> getMapOrEmpty ( final K key )
    {
        final Map<K2, V> map = getMap( key );
        return map != null ? map : new HashMap<>();
    }
}
