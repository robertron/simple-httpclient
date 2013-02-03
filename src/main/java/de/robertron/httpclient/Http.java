package de.robertron.httpclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.util.EntityUtils;

import de.robertron.httpclient.HttpAnswer;
import de.robertron.httpclient.HttpHeader;

public class Http {

    private static final Log LOG = LogFactory.getLog( Http.class );
    private static final int MAX_REPEAT = 10;
    private static final int TIMEOUT = 3;

    private final String url;
    private String cookie;
    private final BasicHttpParams params;
    private final Map<String, String> getParameters;
    private final Map<String, String> postParameters;
    private final Map<String, String> headers;

    public Http( final String url ) {
        this.url = url;
        params = new BasicHttpParams();
        getParameters = new HashMap<String, String>();
        postParameters = new HashMap<String, String>();
        headers = new HashMap<String, String>();
    }

    public Http addGetParam( final String key, final String value ) {
        getParameters.put( key, value );
        return this;
    }

    public Http addPostParam( final String key, final String value ) {
        postParameters.put( key, value );
        return this;
    }

    public Http noRedirects() {
        params.setParameter( "http.protocol.handle-redirects", false );
        return this;
    }

    public Http withCookie( final String cookie ) {
        this.cookie = cookie;
        return this;
    }

    public Http addHeader( final String key, final String value ) {
        headers.put( key, value );
        return this;
    }

    public HttpAnswer get() {
        final HttpGet get = new HttpGet( buildUrl() );
        return call( get, 1 );
    }

    public HttpAnswer post() {
        final HttpPost post = new HttpPost( buildUrl() );
        post.setEntity( buildPostForm() );
        return call( post, 1 );
    }

    private String buildUrl() {
        final StringBuilder concaturl = new StringBuilder( url );
        for ( final Entry<String, String> entry : getParameters.entrySet() ) {
            if ( concaturl.toString().contains( "?" ) ) {
                concaturl.append( "&" );
            } else {
                concaturl.append( "?" );
            }
            concaturl.append( entry.getKey() ).append( "=" ).append( entry.getValue() );
        }
        return concaturl.toString();
    }

    private UrlEncodedFormEntity buildPostForm() {
        final List<NameValuePair> list = new ArrayList<NameValuePair>();
        for ( final Entry<String, String> entry : postParameters.entrySet() ) {
            list.add( new BasicNameValuePair( entry.getKey(), entry.getValue() ) );
        }
        try {
            return new UrlEncodedFormEntity( list, "UTF-8" );
        } catch ( final UnsupportedEncodingException e ) {
            LOG.fatal( e.getMessage(), e );
            System.exit( 1 );
        }
        return null;
    }

    private final HttpAnswer call( final HttpUriRequest request, final int number ) {
        try {
            return callInternal( request, number );
        } catch ( final UnsupportedEncodingException e ) {
            LOG.fatal( e.getMessage(), e );
            System.exit( 1 );
        } catch ( final ParseException e ) {
            LOG.fatal( e.getMessage(), e );
            System.exit( 1 );
        } catch ( final IOException e ) {
            LOG.fatal( e.getMessage(), e );
            System.exit( 1 );
        }
        return null;
    }

    private HttpAnswer callInternal( final HttpUriRequest request, int number ) throws ClientProtocolException, IOException {
        final HttpClient httpclient = client();
        request.setParams( params );
        for ( final Map.Entry<String, String> header : headers.entrySet() ) {
            request.setHeader( header.getKey(), header.getValue() );
        }
        if ( cookie != null ) {
            request.setHeader( "Cookie", cookie );
        }
        final HttpResponse response = httpclient.execute( request );
        final int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode == 503 ) {
            if ( number < MAX_REPEAT ) {
                try {
                    LOG.info( "ERROR: " + statusCode + " nochmal in " + TIMEOUT + " Sekunden zum " + number + ". mal." );
                    Thread.sleep( TIMEOUT * 1000 );
                } catch ( final InterruptedException e ) {
                    LOG.error( e.getMessage(), e );
                }
                return call( request, number++ );
            }
            LOG.info( "Maximale Retry-Anzahl von " + MAX_REPEAT + " erreicht." );
        }

        final HttpEntity responseEntity = response.getEntity();
        final String content = EntityUtils.toString( responseEntity );
        return new HttpAnswer( content, HttpHeader.from( response ) );
    }

    @SuppressWarnings( "deprecation" )
    private static HttpClient client() {
        try {
            final SSLContext ctx = SSLContext.getInstance( "TLS" );
            final X509TrustManager tm = new X509TrustManager() {

                @Override
                public void checkClientTrusted( final X509Certificate[] xcs, final String string ) throws CertificateException {
                }

                @Override
                public void checkServerTrusted( final X509Certificate[] xcs, final String string ) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init( null, new TrustManager[] { tm }, null );
            final SSLSocketFactory ssf = new SSLSocketFactory( ctx );
            ssf.setHostnameVerifier( new X509HostnameVerifier() {

                @Override
                public void verify( final String string, final SSLSocket ssls ) throws IOException {
                }

                @Override
                public void verify( final String string, final X509Certificate xc ) throws SSLException {
                }

                @Override
                public void verify( final String string, final String[] strings, final String[] strings1 ) throws SSLException {
                }

                @Override
                public boolean verify( final String string, final SSLSession ssls ) {
                    return true;
                }
            } );
            final DefaultHttpClient base = new DefaultHttpClient();
            final ClientConnectionManager ccm = base.getConnectionManager();
            final SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register( new Scheme( "https", ssf, 443 ) );
            return new DefaultHttpClient( ccm, base.getParams() );
        } catch ( final Exception e ) {
            LOG.error( e.getMessage(), e );
            System.exit( 1 );
        }
        return null;
    }
}
