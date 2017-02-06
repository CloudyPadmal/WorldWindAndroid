/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wmts;

import android.graphics.Bitmap;

import java.util.LinkedHashSet;
import java.util.Set;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.Retriever;

public class TileImageRequest implements Retriever.Callback {

    protected String url;

    protected Sector tileSector;

    protected Coords tileMatrixIndex;

    protected Set<TileRequest> tileRequestListeners = new LinkedHashSet<>();

    protected Object tileListenerLock = new Object();

    protected Bitmap bitmap;

    public TileImageRequest(String url, Sector tileSector, Coords tileMatrixIndex) {
        this.url = url;
        this.tileSector = tileSector;
        this.tileMatrixIndex = tileMatrixIndex;
    }

    protected void addTileRequestListener(TileRequest tileRequest) {
        synchronized (this.tileListenerLock) {
            this.tileRequestListeners.add(tileRequest);
        }
    }

    @Override
    public void retrievalSucceeded(Retriever retriever, Object key, Object options, Object value) {
        this.bitmap = (Bitmap) value;
        synchronized (TileCompositor.getInstance().ptrLock) {
            TileCompositor.getInstance().pendingImageRequests.remove(this.url);
        }
        synchronized (this.tileListenerLock) {
            for (TileRequest tileRequest : tileRequestListeners) {
                tileRequest.tileComplete(this);
            }
        }
    }

    @Override
    public void retrievalFailed(Retriever retriever, Object key, Throwable ex) {
        Logger.logMessage(Logger.ERROR, "TileImageRequest", "retrievalFailed", "Retrieval failed", ex);
        synchronized (TileCompositor.getInstance().ptrLock) {
            TileCompositor.getInstance().pendingImageRequests.remove(this.url);
        }
        synchronized (this.tileListenerLock) {
            for (TileRequest tileRequest : tileRequestListeners) {
                tileRequest.tileComplete(this);
            }
        }
    }

    @Override
    public void retrievalRejected(Retriever retriever, Object key) {
        Logger.logMessage(Logger.INFO, "TileImageRequest", "retrievalRejected", "Retrieval rejected, retrying attempt...");
        retriever.retrieve(key, null, this);
    }

    public static class Coords {

        public int row;

        public int col;

        public Coords(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TileImageRequest that = (TileImageRequest) o;

        return url.equals(that.url);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
