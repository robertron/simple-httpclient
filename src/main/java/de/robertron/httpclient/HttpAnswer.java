package de.robertron.httpclient;


public class HttpAnswer {

    private final String content;
    private final HttpHeader header;

    public HttpAnswer( final String content, final HttpHeader header ) {
        super();
        this.content = content;
        this.header = header;
    }

    public String getContent() {
        return content;
    }

    public HttpHeader getHeader() {
        return header;
    }
}
