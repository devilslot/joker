package cs.bilkent.joker.operator;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.unmodifiableMap;


/**
 * The tuple is the main data structure to manipulate data in Joker.
 * A tuple is a mapping of keys to values where each value can be any type.
 */
public final class Tuple implements Fields<String>
{

    private final Map<String, Object> values;

    public Tuple ()
    {
        this.values = new HashMap<>();
    }

    public Tuple ( final String key, final Object value )
    {
        checkArgument( value != null, "value can't be null" );
        this.values = new HashMap<>();
        this.values.put( key, value );
    }

    public Tuple ( final Map<String, Object> values )
    {
        checkArgument( values != null, "values can't be null" );
        this.values = new HashMap<>();
        this.values.putAll( values );
    }

    public Map<String, Object> asMap ()
    {
        return unmodifiableMap( values );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T get ( final String key )
    {
        return (T) values.get( key );
    }

    @Override
    public boolean contains ( final String key )
    {
        return values.containsKey( key );
    }

    @Override
    public void set ( final String key, final Object value )
    {
        checkArgument( value != null, "value can't be null" );
        this.values.put( key, value );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> T put ( final String key, final T value )
    {
        checkArgument( value != null, "value can't be null" );
        return (T) this.values.put( key, value );
    }

    @Override
    public Object remove ( final String key )
    {
        return this.values.remove( key );
    }

    @Override
    public boolean delete ( final String key )
    {
        return this.values.remove( key ) != null;
    }

    @Override
    public void clear ()
    {
        this.values.clear();
    }

    @Override
    public int size ()
    {
        return this.values.size();
    }

    @Override
    public Collection<String> keys ()
    {
        return Collections.unmodifiableCollection( values.keySet() );
    }

    @Override
    public boolean equals ( final Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        final Tuple tuple = (Tuple) o;

        return values.equals( tuple.values );

    }

    @Override
    public int hashCode ()
    {
        return values.hashCode();
    }

    @Override
    public String toString ()
    {
        return "Tuple{" + values + '}';
    }

}