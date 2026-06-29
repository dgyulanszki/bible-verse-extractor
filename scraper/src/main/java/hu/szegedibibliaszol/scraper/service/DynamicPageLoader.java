package hu.szegedibibliaszol.scraper.service;

import java.io.IOException;
import org.jsoup.nodes.Document;

interface DynamicPageLoader {

    Document load(String url) throws IOException;

    void close();
}

