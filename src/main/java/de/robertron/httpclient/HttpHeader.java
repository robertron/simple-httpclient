package de.robertron.httpclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import com.google.common.base.Joiner;

public class HttpHeader {

    public Map<String, List<String>> values;

    public HttpHeader() {
        values = new HashMap<String, List<String>>();
    }

    public void add( final String key, final String value ) {
        if ( values.containsKey( key ) ) {
            values.get( key ).add( value );
        } else {
            final List<String> list = new ArrayList<String>();
            list.add( value );
            values.put( key, list );
        }
    }

    public String get( final String key ) {
        return Joiner.on( ";" ).join( values.get( key ) );
    }

    public String get( final String key, final int index ) {
        return values.get( key ).get( index );
    }

    public static HttpHeader from( final HttpResponse response ) {
        final HttpHeader result = new HttpHeader();
        for ( final Header header : response.getAllHeaders() ) {
            result.add( header.getName(), header.getValue() );
        }
        return result;
    }

}