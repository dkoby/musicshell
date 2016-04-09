/*
 *
 */
package mshell.lastfm;
/* */
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
/* */
import mshell.util.DPrint;

/**
 *
 */
public class LastFM {
    private final DPrint.Level debugLevel = DPrint.Level.VERBOSE1;
//    private final DPrint.Level debugLevel = DPrint.Level.EXCEPTION;
    /* */
    private final String host = "ws.audioscrobbler.com";
    private final int port = 80;
    private final String key = "d20db5c186bb668460a8945747ad6dc3";
    /* */
    private final int connectTimeout  = 2000;
    private final int readTimeout     = 5000;
    /* */
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    /* */

    /*
     *
     */
    public LastFM() throws Exception {
        socket = new Socket();

        try {
            socket.connect(new InetSocketAddress(InetAddress.getByName(host), port), connectTimeout);
            socket.setSoTimeout(readTimeout);
        } catch (Exception e) {
            throw e;
        }

        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            try {
                socket.close();
            } catch (Exception eclose) {
                throw eclose;
            }
            throw e;
        }

        try {
            out = new PrintStream(socket.getOutputStream(), true);
        } catch (Exception e) {
            try {
                in.close();
                socket.close();
            } catch (Exception eclose) {
                throw eclose;
            }
            throw e;
        }

        DPrint.format(DPrint.Level.VERBOSE4, "last.fm, connected%n");
    }
    /**
     *
     */
    public String query(String artist, String album) throws Exception {
        String result = null;
        StringBuilder request;
            
        request = new StringBuilder();
        request.append("GET ");
        request.append("/2.0/");
        request.append("?method=album.getinfo");
        request.append("&api_key=" + key);
        request.append("&artist=" + escapeHTML(artist));
        request.append("&album=" + escapeHTML(album));
        request.append("&autocorrect1");
        request.append("HTTP/1.1");
        request.append("\r\n");

        DPrint.format(DPrint.Level.VERBOSE4, "last.fm, request: %s%n", request);

        try {
            out.print(request.toString());
        } catch (Exception e) {
            throw e;
        }

        request = new StringBuilder();
        request.append("Host: ws.audioscrobbler.com\r\n");
        request.append("User-Agent : tick/1.0\r\n");
        request.append("\r\n");

        try {
            out.print(request.toString());
        } catch (Exception e) {
            throw e;
        }

        while (true)
        {
            String line;
            try {
                line = in.readLine();
                if (line == null)
                    break;

                DPrint.format(debugLevel, "Response \"%s\"%n", line);

                Matcher matcher = Pattern.compile(
                        "^<image\\s+size=\"(small|medium|large|extralarge|mega)\">\\s*(.*)\\s*</image>"
                        ).matcher(line);
                if (matcher.find() && matcher.start(2) >= 0) {
                    if (matcher.group(2).length() > 0)
                        result = new String(matcher.group(2));
                    continue;
                }
            } catch (SocketTimeoutException e) {
                throw (Exception)new Exception("Timeout").initCause(e);
            } catch (Exception e) {
                throw e;
            }
        } 

        return result;
    }
    /**
     *
     */
    private String escapeHTML(String string) {
        StringBuilder result = new StringBuilder();
        /* XXX UTF-8 valid ? */
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '!' : result.append("%21"); break;
                case '#' : result.append("%23"); break;
                case '$' : result.append("%24"); break;
                case '&' : result.append("%26"); break;
                case '\'': result.append("%27"); break;
                case '(' : result.append("%28"); break;
                case ')' : result.append("%29"); break;
                case '*' : result.append("%2A"); break;
                case '+' : result.append("%2B"); break;
                case ',' : result.append("%2C"); break;
                case '/' : result.append("%2F"); break;
                case ':' : result.append("%3A"); break;
                case ';' : result.append("%3B"); break;
                case '=' : result.append("%3D"); break;
                case '?' : result.append("%3F"); break;
                case '@' : result.append("%40"); break;
                case '[' : result.append("%5B"); break;
                case ']' : result.append("%5D"); break;
                case ' ' : result.append("%20"); break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Close connection to MPD
     */
    public void close() {
        try {
            in.close();
            out.close();
            socket.close();
        } catch(Exception e) {
            System.out.println("Failed to close: " + e.getMessage());
        }
    }
}

//        try {
//            Arrays.fill(mpdVersion, 0);
//
//            Pattern pattern = Pattern.compile("^OK\\s+MPD\\s+(\\d+)\\.(\\d+)\\.(\\d+)");
//            Matcher matcher = pattern.matcher(in.readLine());
//            if (matcher.find()) {
//                if (matcher.start(1) >= 0) mpdVersion[0] = Integer.decode(matcher.group(1));
//                if (matcher.start(2) >= 0) mpdVersion[1] = Integer.decode(matcher.group(2));
//                if (matcher.start(3) >= 0) mpdVersion[2] = Integer.decode(matcher.group(3));
//                DPrint.format(DPrint.Level.VERBOSE0, "Version %d.%d.%d%n",
//                        mpdVersion[0], mpdVersion[1], mpdVersion[2]);
//            } else {
//                throw new MPDException("Invalid MPD Response");
//            }
//        } catch (Exception e) {
//            try {
//                in.close();
//                out.close();
//                socket.close();
//            } catch (Exception eclose) {
//                throw (MPDException)new MPDException(eclose.toString()).initCause(eclose);
//            }
//            throw (MPDException)new MPDException(e.toString()).initCause(e);
//        }
