package ch.windmobile.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import ch.windmobile.R;

public class RestClient {

    private final Context context;
    private final int connectionTimeout;
    private final int socketTimeout;

    private String userAgent;

    public RestClient(Context context) {
        // Infinite timeout
        this.connectionTimeout = 0;
        this.socketTimeout = 0;
        this.context = context;
    }

    public RestClient(Context context, int connectionTimeout, int socketTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.context = context;
    }

    public JSONObject get(String url) throws ServerException, ClientProtocolException, IOException, JSONException {
        HttpParams httpParameters = new BasicHttpParams();
        // Set connection timeout and socket timeout
        HttpConnectionParams.setConnectionTimeout(httpParameters, getConnectionTimeout());
        HttpConnectionParams.setSoTimeout(httpParameters, getSocketTimeout());
        HttpClient httpClient = new DefaultHttpClient(httpParameters);

        // Prepare a request object
        HttpGet httpGet = new HttpGet(url);
        if (getUserAgent() != null) {
            httpGet.setHeader("User-Agent", getUserAgent());
        }
        // Ask for JSON
        httpGet.setHeader("Accept", "application/json");

        // Execute the request
        HttpResponse response = httpClient.execute(httpGet);

        // Get hold of the response entity
        HttpEntity entity = response.getEntity();

        JSONObject jsonObject = null;

        // If the response does not enclose an entity, there is no need
        // to worry about connection release
        if (entity != null) {
            InputStream inStream = entity.getContent();
            String result = convertStreamToString(inStream);

            jsonObject = new JSONObject(result);
        }

        int httpStatusCode = response.getStatusLine().getStatusCode();
        if (httpStatusCode == HttpStatus.SC_OK) {
            return jsonObject;
        } else {
            if (jsonObject != null) {
                String serverMessage = jsonObject.getString("message");
                Log.e("WindMobile", "RestClient.get('" + url + "') --> Http status code = '" + httpStatusCode + "', message = '" + serverMessage
                    + "'");

                int errorCode = jsonObject.getInt("code");
                CharSequence localizedName;
                switch (errorCode) {
                case -4:
                    localizedName = getContext().getText(R.string.server_connection_error);
                    break;

                case -3:
                    localizedName = getContext().getText(R.string.server_invalid_data);
                    break;

                case -2:
                    localizedName = getContext().getText(R.string.server_database_error);
                    break;

                default:
                    localizedName = getContext().getText(R.string.server_unknown_error);
                    break;
                }
                throw new ServerException(localizedName, serverMessage);
            } else {
                Log.e("WindMobile", "RestClient.get('" + url + "') --> Http status code = '" + httpStatusCode + "', no error content");
                return null;
            }
        }
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public Context getContext() {
        return context;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    private static String convertStreamToString(InputStream inStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } finally {
            try {
                inStream.close();
            } catch (IOException e) {
            }
        }

        return sb.toString();
    }
}