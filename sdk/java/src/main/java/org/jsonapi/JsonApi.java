package org.jsonapi;

import com.google.gson.Gson;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: zpenzeli
 * Date: 22/05/12
 * Time: 14:43
 */
public class JsonApi {

    private static Logger logger = Logger.getLogger(JsonApi.class);

    private String hostname;
    private String port;
    private String username;
    private String password;
    private String salt;

    private static String URL_FORMAT_FOR_CALL = "http://{host}:{port}/api/call?method={method}&args{args}&key={key}";

    public JsonApi(String _hostname, int _port, String _username, String _password, String _salt) {
        hostname = _hostname;
        port = Integer.toString(_port);

        username = _username;
        password = _password;

        salt = _salt;

    }

    public String call(String method) {
        return call(method, new String[0]);
    }

    public String call(String method, String[] args) {

        URL url;
        try {
            url = new URL(makeURL(method, args));
        } catch (MalformedURLException e) {
            logger.error("Cannot call URL", e);
            return e.getMessage();
        }

        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                logger.error("Response from remote server indicates an error");
                return "Response from remote server indicates an error";
            }

            InputStreamReader is = new InputStreamReader(connection.getInputStream());

            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(is);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }

            return writer.toString();

        } catch (IOException e) {
            logger.error("Cannot connect to assembled URL", e);
            return e.getMessage();
        }

    }

    private String makeURL(String method, String[] args) {

        Gson gson = new Gson();
        String retVal = null;
        try {
            retVal =
                    URL_FORMAT_FOR_CALL.replace("{host}", hostname)
                            .replace("{port}", port)
                            .replace("{method}", URLEncoder.encode(method, "UTF-8"))
                            .replace("{args}", args.length > 0 ? "=" + URLEncoder.encode(gson.toJson(args), "UTF-8") : "")
                            .replace("{key}", createKey(method));
        } catch (UnsupportedEncodingException ignored) {
        }

        return retVal;

    }

    private String createKey(String methodName) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");

        } catch (NoSuchAlgorithmException ignored) {
            return null;
        }

        String stringToHash = username + methodName + password + salt;

        md.update(stringToHash.getBytes());

        byte fullByteHash[] = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte aByteHash : fullByteHash) {
            sb.append(Integer.toString((aByteHash & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();

    }


    public static void main(String[] args) {

        logger.debug("Starting");

        JsonApi jsonApi = new JsonApi("mc.tregele.com", 38591, "patr0n", "KJIUbege83rj", "se4kui7s64gehtu67gs34t8s6g4tm");

        logger.info("Response: " + jsonApi.call("getPlayerCount"));


    }


}
