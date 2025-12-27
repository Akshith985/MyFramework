package com.simpleweb;
import java.io.IOException;
public interface Handler {
    void handle(Context ctx) throws IOException;
}
