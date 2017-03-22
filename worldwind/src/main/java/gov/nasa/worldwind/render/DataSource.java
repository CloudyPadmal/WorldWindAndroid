/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.Logger;

public class DataSource {

    protected static final int TYPE_UNRECOGNIZED = 0;

    protected static final int TYPE_URL = 1;

    protected int type = TYPE_UNRECOGNIZED;

    protected Object source;

    public DataSource() {
    }

    public static DataSource fromUrl(String urlString) {
        if (urlString == null) {
            throw new IllegalArgumentException(
                Logger.logMessage(Logger.ERROR, "DataSource", "fromUrl", "missingUrl"));
        }

        DataSource dataSource = new DataSource();
        dataSource.type = TYPE_URL;
        dataSource.source = urlString;
        return dataSource;
    }
}
