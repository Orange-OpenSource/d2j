/* Software Name : DalvikToJimple
 * Version : 1.0
 *
 * Copyright © 2010 France Télécom
 * All rights reserved.
 */

package com.orange.d2j;

/*
 * #%L
 * D2J
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2009 - 2014 Orange SA
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for a compiled XML resource file.
 * @author Pierre Cregut
 *
 */
/**
 * @author Pierre Cregut
 */
public class DalvikResource implements XMLReader, Locator {

    /** Nothing. */
    public static final int RES_NULL_TYPE = 0x0000;

    /** A string pool. */
    public static final int RES_STRING_POOL_TYPE = 0x0001;

    /** A resource table. */
    public static final int RES_TABLE_TYPE = 0x0002;

    /** An XML tree. */
    public static final int RES_XML_TYPE = 0x0003;

    /** Start namespace translation. */
    public static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;

    /** End namespace translation. */
    public static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;

    /** Start elemnt. */
    public static final int RES_XML_START_ELEMENT_TYPE = 0x0102;

    /** End element. */
    public static final int RES_XML_END_ELEMENT_TYPE = 0x0103;

    /** CDATA. */
    public static final int RES_XML_CDATA_TYPE = 0x0104;

    /**
     * Map of resource. This contains a uint32_t array mapping strings in the
     * string pool back to resource identifiers. It is optional.
     */
    public static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

    /** package. */
    public static final int RES_TABLE_PACKAGE_TYPE = 0x0200;

    /** type. */
    public static final int RES_TABLE_TYPE_TYPE = 0x0201;

    /** spec of type. */
    public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;

    /** Is the table sorted. */
    public static final int SORTED_FLAG = 0x01;

    /** Coding for the string table (UTF8 or Unicode). */
    public static final int UTF8_FLAG = 0x100;

    /** The Constant FEATURE_NAMESPACE. */
    final static String FEATURE_NAMESPACE = "http://xml.org/sax/features/namespaces";

    /** The Constant FEATURE_NS_PREFIXES. */
    final static String FEATURE_NS_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";

    /** The Constant CDATA. */
    final static String CDATA = "CDATA";

    /** Is the string pool sorted ?. */
    private boolean sortedStringPool;

    /** The error handler. */
    private ErrorHandler errorHandler;

    /** The content handler. */
    private ContentHandler contentHandler;

    /** The current line. */
    private int currentLine;

    /** The reader. */
    private DalvikValueReader reader;

    /** The strings. */
    private String[] strings;

    /** The styles. */
    private StyleSpan[] styles;

    /** The current_namespaces. */
    private final Stack<String> current_namespaces = new Stack<String>();

    /**
     * A style span in an Android resource.
     * 
     * @author Pierre Cregut
     */
    public static class StyleSpan {

        /** Style name. */
        private final String name;

        /** Start of the span. */
        private final int start;

        /** End of the span. */
        private final int end;

        /**
         * Instantiates a new style span.
         * 
         * @param name the name
         * @param start the start
         * @param end the end
         */
        StyleSpan(String name, int start, int end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        /**
         * Gets the name.
         * 
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the start where the style applies
         * 
         * @return the start
         */
        public int getStart() {
            return start;
        }

        /**
         * Gets the end position
         * 
         * @return the end
         */
        public int getEnd() {
            return end;
        }
    }

    /**
     * Read header.
     * 
     * @param stream the stream
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private void readHeader(InputStream stream) throws IOException {
        reader = new DalvikValueReader(stream, 4);
        reader.ushort(); // Element kind. f
        int sz = reader.ushort(); // header size
        reader.uint(); // total size: do not rely on it = 0
        reader.seek(sz);
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#parse(org.xml.sax.InputSource)
     */
    @Override
    public void parse(InputSource source) throws IOException, SAXException {
        InputStream is = source.getByteStream();
        if (is == null)
            throw new SAXException("need a regular inputstream");
        parse(is);
    }

    /**
     * Main parsing function at the level of the inputstream.
     * 
     * @param is the is
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws SAXException the sAX exception
     */
    public void parse(InputStream is) throws IOException, SAXException {
        readHeader(is);
        readStringPool();
        if (contentHandler != null) {
            contentHandler.setDocumentLocator(this);
            contentHandler.startDocument();
        }
        try {
            while (reader.hasMore())
                readXmlChunk();
        } catch (RuntimeException e) {
            e.printStackTrace();
            D2JLog.warning("Runtime Exception while reading a chunk\n" + e.getMessage());
        }
        if (contentHandler != null)
            contentHandler.endDocument();
    }

    /**
     * Read string pool.
     */
    private void readStringPool() {
        int start_pos = reader.getPos();
        if (reader.ushort() != RES_STRING_POOL_TYPE) {
            throw new RuntimeException("Not a string pool");
        }
        if (reader.ushort() != 0x1c) {
            throw new RuntimeException("Strange string pool header size");
        }
        int pool_size = (int) reader.uint(); // pool size.
        int string_count = reader.sint();
        int style_count = reader.sint();
        strings = new String[string_count];
        styles = new StyleSpan[style_count];
        int flags = reader.sint();
        sortedStringPool = (flags & SORTED_FLAG) != 0;
        boolean utf8 = (flags & UTF8_FLAG) != 0;
        int strings_start = reader.sint();
        int style_offset = reader.sint();
        String string;
        int str_size, str_length, start, end;
        int[] offset_strings = new int[string_count];
        for (int i = 0; i < string_count; i++) {
            offset_strings[i] = (int) reader.uint();
        }
        for (int i = 0; i < string_count; i++) {
            if (utf8) {
                reader.seek(start_pos + strings_start + offset_strings[i]);
                reader.uleb128(); // str_size
                str_length = (int) reader.uleb128();
                string = reader.utf8String();
                strings[i] = string;
                if (string.length() != str_length)
                    throw new RuntimeException("Mismatched size for strings " + string + " "
                            + str_length + " / " + string.length());
            } else {
                reader.seek(start_pos + strings_start + offset_strings[i]);
                str_size = (int) reader.uleb128_16();
                string = reader.unicodeString(str_size);
                strings[i] = string;
            }
            if (D2JLog.stringPool)
                D2JLog.info(i + "\t:\t" + strings[i]);
        }
        if (style_offset != 0) {
            reader.seek(start_pos + style_offset);
            for (int i = 0; i < style_count; i++) {
                string = getString();
                start = (int) reader.uint();
                end = (int) reader.uint();
                styles[i] = new StyleSpan(string, start, end);
            }
        }
        reader.seek(start_pos + pool_size);
    }

    /**
     * Gets the string.
     * 
     * @return the string
     */
    String getString() {
        int v = (int) reader.uint();
        if (v == -1)
            return null;
        try {
            return strings[v];
        } catch (ArrayIndexOutOfBoundsException e) {
            D2JLog.warning("Bogus string index");
            return "";
        }
    }

    /**
     * Read xml chunk.
     * 
     * @throws SAXException the sAX exception
     */
    void readXmlChunk() throws SAXException {
        int pos = reader.getPos();
        int kind = reader.ushort();
        reader.ushort(); // Header size
        int size = (int) reader.uint(); // size
        switch (kind) {
            case RES_XML_RESOURCE_MAP_TYPE:
                if (D2JLog.resources) {
                    D2JLog.info("resource map type " + size);
                    int n = (size - 8) / 4;
                    for (int i = 0; i < n; i++)
                        D2JLog.info(i + " : " + Integer.toHexString((int) reader.uint()));
                }
                reader.seek(pos + size);
                break;
            case RES_XML_START_NAMESPACE_TYPE:
                currentLine = (int) reader.uint();
                getString(); // comments
                String prefix = getString();
                String uri = getString();
                if (D2JLog.resources)
                    D2JLog.info("N:" + prefix + "=" + uri + "(" + currentLine + ")");
                current_namespaces.push(uri);
                if (contentHandler != null)
                    contentHandler.startPrefixMapping(prefix, uri);
                break;
            case RES_XML_END_NAMESPACE_TYPE:
                currentLine = (int) reader.uint();
                getString(); // comments
                prefix = getString();
                uri = getString();
                if (D2JLog.resources)
                    D2JLog.info("/N:" + prefix + "=" + uri);
                current_namespaces.pop();
                if (contentHandler != null)
                    contentHandler.endPrefixMapping(prefix);
                break;
            case RES_XML_START_ELEMENT_TYPE:
                currentLine = (int) reader.uint();
                getString(); // comments
                String recorded_ns = current_namespaces.peek();
                String namespace = getString();
                namespace = (namespace == null) ? recorded_ns : namespace;
                String name = getString();
                if (D2JLog.resources)
                    D2JLog.info("E:" + name);
                reader.ushort(); // attribute offset
                reader.ushort(); // attributes size
                int attr_count = reader.ushort();

                reader.ushort(); // id index
                reader.ushort(); // class index
                reader.ushort(); // style index
                AttributesImpl atts = new AttributesImpl();
                for (int i = 0; i < attr_count; i++) {
                    String namespaceAttr = getString();

                    namespaceAttr = (namespaceAttr == null) ? recorded_ns : namespaceAttr;
                    String nameAttr = getString();
                    String raw = getString(); // raw value.
                    ResourceData typedValueAttr = ResourceData.parseData(reader, strings);
                    String v = typedValueAttr.toString();
                    atts.addAttribute(namespaceAttr, nameAttr, null, CDATA, v);
                    if (D2JLog.resources)
                        D2JLog.info("  A:" + nameAttr + "=" + raw + typedValueAttr);
                }
                if (contentHandler != null)
                    contentHandler.startElement(namespace, name, null, atts);
                break;
            case RES_XML_END_ELEMENT_TYPE:
                currentLine = (int) reader.uint();
                recorded_ns = current_namespaces.peek();
                getString(); // comments
                namespace = getString();
                namespace = (namespace == null) ? recorded_ns : namespace;
                name = getString();
                if (D2JLog.resources)
                    D2JLog.info("/E:" + name + "(" + currentLine + ")");
                if (contentHandler != null)
                    contentHandler.endElement(namespace, name, null);
                break;
            case RES_XML_CDATA_TYPE:
                currentLine = (int) reader.uint();
                getString(); // comments
                String rawData = getString(); // Raw cdata
                if (D2JLog.resources)
                    D2JLog.info("Raw data " + rawData);
                ResourceData data = ResourceData.parseData(reader, strings);
                char[] cdata2 = data.toString().toCharArray();
                if (D2JLog.resources)
                    D2JLog.info("C:" + new String(cdata2));
                if (contentHandler != null) {
                    contentHandler.characters(cdata2, 0, cdata2.length);
                }
                break;
            default:
                D2JLog.warning("The kind " + kind + " is not treated for resource files.");
        }
    }

    /**
     * The Class TestContentHandler.
     */
    static class TestContentHandler extends DefaultHandler {

        /** The depth. */
        int depth = 0;

        /** The pf map. */
        HashMap<String, String> pfMap = new HashMap<String, String>();

        /** The pf rev map. */
        HashMap<String, String> pfRevMap = new HashMap<String, String>();

        /*
         * (non-Javadoc)
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
         * java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (D2JLog.resources) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < depth; i++)
                    b.append("  ");
                b.append("</").append(solve(uri)).append(":").append(localName).append(">");
                D2JLog.info(b.toString());
            }
            depth--;
        }

        /*
         * (non-Javadoc)
         * @see
         * org.xml.sax.helpers.DefaultHandler#endPrefixMapping(java.lang.String)
         */
        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            if (D2JLog.resources)
                D2JLog.info("</ns prefix=" + prefix + ">");
            String uri = pfMap.get(prefix);
            pfMap.remove(prefix);
            pfRevMap.remove(uri);
        }

        /*
         * (non-Javadoc)
         * @see
         * org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax
         * .Locator)
         */
        @Override
        public void setDocumentLocator(Locator locator) {
        }

        /**
         * Solve.
         * 
         * @param uri the uri
         * @return the string
         */
        private String solve(String uri) {
            String pfx = pfRevMap.get(uri);
            return (pfx == null) ? uri : pfx;
        }

        /*
         * (non-Javadoc)
         * @see
         * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
         * java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts)
                throws SAXException {
            depth++;
            if (D2JLog.resources) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < depth; i++)
                    b.append("  ");
                b.append("<").append(solve(uri)).append(":").append(localName);
                for (int i = 0; i < atts.getLength(); i++) {
                    b.append(" ").append(solve(atts.getURI(i))).append(":")
                            .append(atts.getLocalName(i)).append("=\"").append(atts.getValue(i))
                            .append("\"");
                }
                b.append(">");
                D2JLog.info(b.toString());
            }

        }

        /*
         * (non-Javadoc)
         * @see
         * org.xml.sax.helpers.DefaultHandler#startPrefixMapping(java.lang.String
         * , java.lang.String)
         */
        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            pfMap.put(prefix, uri);
            pfRevMap.put(uri, prefix);
            if (D2JLog.resources)
                D2JLog.info("<ns prefix=" + prefix + " uri=" + uri + ">");
        }

    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getContentHandler()
     */
    @Override
    public ContentHandler getContentHandler() {
        return contentHandler;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getDTDHandler()
     */
    @Override
    public DTDHandler getDTDHandler() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getEntityResolver()
     */
    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getErrorHandler()
     */
    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getFeature(java.lang.String)
     */
    @Override
    public boolean getFeature(String arg) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if (arg.equals(FEATURE_NAMESPACE))
            return true;
        if (arg.equals(FEATURE_NS_PREFIXES))
            return false;
        throw new SAXNotRecognizedException("No such feature: " + arg);
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#getProperty(java.lang.String)
     */
    @Override
    public Object getProperty(String arg) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        throw new SAXNotRecognizedException("No properties: " + arg);
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#parse(java.lang.String)
     */
    @Override
    public void parse(String systemId) throws IOException, SAXException {
        parse(new InputSource(systemId));
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setContentHandler(org.xml.sax.ContentHandler)
     */
    @Override
    public void setContentHandler(ContentHandler arg) {
        contentHandler = arg;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setDTDHandler(org.xml.sax.DTDHandler)
     */
    @Override
    public void setDTDHandler(DTDHandler arg0) {
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setEntityResolver(org.xml.sax.EntityResolver)
     */
    @Override
    public void setEntityResolver(EntityResolver arg0) {
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setErrorHandler(org.xml.sax.ErrorHandler)
     */
    @Override
    public void setErrorHandler(ErrorHandler arg) {
        errorHandler = arg;

    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    @Override
    public void setFeature(String feature, boolean state) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        if (feature.equals(FEATURE_NAMESPACE) || feature.equals(FEATURE_NS_PREFIXES))
            return;
        throw new SAXNotRecognizedException("No such feature: " + feature);
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.XMLReader#setProperty(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void setProperty(String arg0, Object arg1) throws SAXNotRecognizedException,
            SAXNotSupportedException {
        throw new SAXNotRecognizedException("No properties: " + arg0);
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.Locator#getColumnNumber()
     */
    @Override
    public int getColumnNumber() {
        return -1;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.Locator#getLineNumber()
     */
    @Override
    public int getLineNumber() {
        return currentLine;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.Locator#getPublicId()
     */
    @Override
    public String getPublicId() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see org.xml.sax.Locator#getSystemId()
     */
    @Override
    public String getSystemId() {
        return null;
    }

    /**
     * Entry point for tests.
     * 
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String args[]) throws Exception {
        D2JLog.resources = true;
        InputStream is;

        String filename = (args.length == 0) ? new File(System.getProperty("user.home"),
                "AndroidManifest.xml").getAbsolutePath() : args[0];
        if (filename.endsWith(".apk")) {

        } else {
            is = new FileInputStream(filename);
            try {
                DalvikResource df = new DalvikResource();
                df.setContentHandler(new TestContentHandler());
                df.parse(is);
            } finally {
                is.close();
            }
        }

    }

    /**
     * Check if string pool sorted.
     * 
     * @return true, if is sorted string pool
     */
    public boolean isSortedStringPool() {
        return sortedStringPool;
    }

}
