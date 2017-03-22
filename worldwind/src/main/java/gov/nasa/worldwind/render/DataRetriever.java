/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import gov.nasa.worldwind.util.Retriever;
import gov.nasa.worldwind.util.WWUtil;

public class DataRetriever extends Retriever<DataSource, DataOptions, ByteBuffer> {

    public DataRetriever(int maxSimultaneousRetrievals) {
        super(maxSimultaneousRetrievals);
    }

    @Override
    protected void retrieveAsync(DataSource key, DataOptions options, Callback<DataSource, DataOptions, ByteBuffer>
        callback) {
        try {
            ByteBuffer data = this.decodeData(key);

            if (data != null) {
                callback.retrievalSucceeded(this, key, options, data);
            } else {
                callback.retrievalFailed(this, key, null); // failed but no exception
            }
        } catch (Throwable logged) {
            callback.retrievalFailed(this, key, logged); // failed with exception
        }
    }

    protected ByteBuffer decodeData(DataSource dataSource) throws Exception {
        if (dataSource.type == DataSource.TYPE_URL) {
            return this.decodeUrl((String) dataSource.source);
        } else {
            return null;
        }
    }

    protected ByteBuffer decodeUrl(String url) throws Exception {

        InputStream stream = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(30000);

            // TODO check this is the best way for getting the byte array
            stream = new BufferedInputStream(conn.getInputStream());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int nRead;
            byte[] dataBuffer = new byte[4096];

            while ((nRead = stream.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                baos.write(dataBuffer, 0, nRead);
            }

            baos.flush();
            return ByteBuffer.wrap(baos.toByteArray());
        } finally {
            WWUtil.closeSilently(stream);
        }
    }
}
