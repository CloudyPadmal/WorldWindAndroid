/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wmts;

import android.graphics.Bitmap;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ImageSource;
import gov.nasa.worldwind.render.ImageTile;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSetConfig;

public class TileRequest implements Runnable {

    public static String TILEMATRIX_TEMPLATE = "{TileMatrix}";

    public static String TILEROW_TEMPLATE = "{TileRow}";

    public static String TILECOL_TEMPLATE = "{TileCol}";

    public static double NEAR_ZERO = 1e-6;

    public static Bitmap COMPOSITE = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888);

    public static int[] PIXELS = new int[1024 * 1024];

    /**
     * The World Wind requested Sector.
     */
    protected Sector requestedSector;

    protected WmtsTileMatrixSet tileMatrixSet;

    /**
     * The Sector describing the bounding box of the Tile Matrix Set. Determined from either the Layer WGS84BoundingBox,
     * the Layer BoundingBox with matching CRS, or the Tile Matrix Set defined bounding box.
     */
    protected Sector tileMatrixSetBoundingBox;

    /**
     * The image tile to update with the sampled Bitmap.
     */
    protected ImageTile imageTile;

    /**
     * The image size of the bitmap returned to the provided image tile.
     */
    protected int desiredImageSize;

    protected Level level;

    protected String urlTemplate;

    protected Map<WmtsTileMatrix, Position> tileMatrixTopLeftCorners;

    protected int tileMatrixImageHeight;

    protected int tileMatrixImageWidth;

    protected Sector stitchSector;

    protected int stitchRows;

    protected int stitchCols;

    protected int stitchStartRow;

    protected int stitchStartCol;

    protected TileImageRequest[] tiles;

    protected AtomicInteger completedTilesCount = new AtomicInteger(0);

    protected static LevelSetConfig LEVEL_SET_CONFIG = new LevelSetConfig();

    public TileRequest(Sector requestedSector, WmtsTileMatrixSet tileMatrixSet, Sector tileMatrixSetBoundingBox,
                       ImageTile imageTile, int desiredImageSize, Level level, String urlTemplate,
                       Map<WmtsTileMatrix, Position> tileMatrixTopLeftCorners) {
        this.requestedSector = requestedSector;
        this.tileMatrixSet = tileMatrixSet;
        this.tileMatrixSetBoundingBox = tileMatrixSetBoundingBox;
        this.imageTile = imageTile;
        this.desiredImageSize = desiredImageSize;
        this.level = level;
        this.urlTemplate = urlTemplate;
        this.tileMatrixTopLeftCorners = tileMatrixTopLeftCorners;
    }

    @Override
    public void run() {
        if (this.tiles == null) {
            // First check if this thing has already been submitted, now that we are on our own thread no liveliness issues
            synchronized (TileCompositor.getInstance().ptrLock) {
                if (TileCompositor.getInstance().pendingTileRequests.contains(this)) {
                    // don't do anything it is already being worked on
                    return;
                } else {
                    TileCompositor.getInstance().pendingTileRequests.add(this);
                }
            }
            this.buildStitch();
        } else {
            this.done();
        }
    }

    protected void buildStitch() {
        WmtsTileMatrix tileMatrix = this.determineSuitableTileMatrix();
        this.tileMatrixImageHeight = tileMatrix.getTileHeight();
        this.tileMatrixImageWidth = tileMatrix.getTileWidth();
        this.createTileImageRequests(tileMatrix);
    }

    protected WmtsTileMatrix determineSuitableTileMatrix() {
        // determine suitable tile matrix given scale denominator
        for (int i = 0; i < this.tileMatrixSet.getTileMatrices().size(); i++) {
            WmtsTileMatrix tileMatrix = this.tileMatrixSet.getTileMatrices().get(i);
            LEVEL_SET_CONFIG.tileWidth = tileMatrix.getTileWidth();
            LEVEL_SET_CONFIG.tileHeight = tileMatrix.getTileHeight();
            double minMetersPerPixel = tileMatrix.getScaleDenominator() * 0.00028;
            double minRadiansPerPixel = minMetersPerPixel / WorldWind.WGS84_SEMI_MAJOR_AXIS;
            int scaleLevel = LEVEL_SET_CONFIG.numLevelsForResolution(minRadiansPerPixel) - 1;
            if (scaleLevel > this.level.levelNumber) {
                return this.tileMatrixSet.getTileMatrices().get(Math.max(0, i - 1));
            }
        }

        return null;
    }

    protected void createTileImageRequests(WmtsTileMatrix tileMatrix) {

        Position topLeftCorner = this.tileMatrixTopLeftCorners.get(tileMatrix);

        double minLon = Math.max(this.tileMatrixSetBoundingBox.minLongitude(), topLeftCorner.longitude);
        double maxLat = Math.min(this.tileMatrixSetBoundingBox.maxLatitude(), topLeftCorner.latitude);

        double deltaX = this.tileMatrixSetBoundingBox.maxLongitude() - minLon;
        double deltaY = maxLat - this.tileMatrixSetBoundingBox.minLatitude();

        double tileGeoWidth = deltaX / tileMatrix.getMatrixWidth();
        double tileGeoHeight = deltaY / tileMatrix.getMatrixHeight();

        double minLonDiff = this.requestedSector.minLongitude() - minLon;
        double maxLonDiff = this.requestedSector.maxLongitude() - minLon;
        double minLatDiff = maxLat - this.requestedSector.maxLatitude();
        double maxLatDiff = maxLat - this.requestedSector.minLatitude();

        int tileMinCol;
        if (Math.abs(minLonDiff) < NEAR_ZERO) {
            tileMinCol = 0;
        } else {
            tileMinCol = (int) (minLonDiff / tileGeoWidth);
        }
        int tileMaxCol;
        if (Math.abs(maxLonDiff) < NEAR_ZERO) {
            tileMaxCol = 0;
        } else {
            tileMaxCol = (int) (maxLonDiff / tileGeoWidth);
        }
        int tileMinRow;
        if (Math.abs(minLatDiff) < NEAR_ZERO) {
            tileMinRow = 0;
        } else {
            tileMinRow = (int) (minLatDiff / tileGeoHeight);
        }
        int tileMaxRow;
        if (Math.abs(maxLatDiff) < NEAR_ZERO) {
            tileMaxRow = 0;
        } else {
            tileMaxRow = (int) (maxLatDiff / tileGeoHeight);
        }

        this.stitchSector = new Sector().setEmpty();
        this.stitchRows = tileMaxRow - tileMinRow + 1;
        this.stitchCols = tileMaxCol - tileMinCol + 1;
        this.stitchStartRow = tileMinRow;
        this.stitchStartCol = tileMinCol;

        this.tiles = new TileImageRequest[(tileMaxRow - tileMinRow + 1) * (tileMaxCol - tileMinCol + 1)];
        synchronized (TileCompositor.getInstance().ptirLock) {
            for (int i = tileMinRow; i <= tileMaxRow; i++) {
                for (int j = tileMinCol; j <= tileMaxCol; j++) {

                    String url = this.urlForTile(i, j, tileMatrix.getIdentifier());
                    TileImageRequest.Coords tileMatrixIndex = new TileImageRequest.Coords(i, j);
                    double minLongitude = minLon + (tileGeoWidth * j);
                    double minLatitude = maxLat - (tileGeoHeight * (i + 1));
                    Sector tileSector = new Sector(minLatitude, minLongitude, tileGeoHeight, tileGeoWidth);
                    this.stitchSector.union(tileSector);

                    TileImageRequest tileImageRequest = new TileImageRequest(url, tileSector, tileMatrixIndex);

                    if (TileCompositor.getInstance().pendingImageRequests.containsKey(url)) {
                        tileImageRequest = TileCompositor.getInstance().pendingImageRequests.get(url);
                        tileImageRequest.addTileRequestListener(this);
                    } else {
                        tileImageRequest.addTileRequestListener(this);
                        TileCompositor.getInstance().pendingImageRequests.put(url, tileImageRequest);
                        ImageSource imageSource = ImageSource.fromUrl(url);
                        TileCompositor.getInstance().imageRetriever.retrieve(imageSource, null, tileImageRequest);
                    }
                }
            }
        }
    }

    protected String urlForTile(int row, int column, String tileMatrixIdentifier) {

        String url = this.urlTemplate.replace(TILEMATRIX_TEMPLATE, tileMatrixIdentifier);
        url = url.replace(TILEROW_TEMPLATE, row + "");
        url = url.replace(TILECOL_TEMPLATE, column + "");

        return url;
    }

    protected void tileComplete(TileImageRequest tileImageRequest) {
        int index = this.completedTilesCount.getAndIncrement();
        this.tiles[index] = tileImageRequest;
        if (this.completedTilesCount.get() == this.tiles.length) {
            TileCompositor.getInstance().service.submit(this);
        }
    }

    protected void done() {
        try {
            int completeStitchWidth = this.stitchCols * this.tileMatrixImageWidth;
            int completeStitchHeight = this.stitchRows * this.tileMatrixImageHeight;

            if ((completeStitchWidth > COMPOSITE.getWidth()) || (completeStitchHeight > COMPOSITE.getHeight())) {
                this.enlargeBitmapResource(Math.max(completeStitchHeight, completeStitchWidth));
            }

            // Fill in the stitchBitmap with the tiles
            for (int i = 0; i < this.tiles.length; i++) {
                if (this.tiles[i].bitmap == null) {
                    continue;
                }
                TileImageRequest tileImageRequest = this.tiles[i];
                tileImageRequest.bitmap.getPixels(PIXELS, 0, this.tileMatrixImageWidth, 0, 0, this.tileMatrixImageWidth, this.tileMatrixImageHeight);
                // Determine start indexes
                int x = (tileImageRequest.tileMatrixIndex.col - this.stitchStartCol) * this.tileMatrixImageWidth;
                int y = (tileImageRequest.tileMatrixIndex.row - this.stitchStartRow) * this.tileMatrixImageHeight;
                COMPOSITE.setPixels(PIXELS, 0, this.tileMatrixImageWidth, x, y, this.tileMatrixImageWidth, this.tileMatrixImageHeight);
            }

            // Convert our requested sector to x and y in pixel coordinates
            int xStart = (int) (((this.requestedSector.minLongitude() - this.stitchSector.minLongitude()) / this.stitchSector.deltaLongitude()) * completeStitchWidth);
            int yStart = (int) (((this.stitchSector.maxLatitude() - this.requestedSector.maxLatitude()) / this.stitchSector.deltaLatitude()) * completeStitchHeight);
            int width = (int) (this.requestedSector.deltaLongitude() / this.stitchSector.deltaLongitude() * completeStitchWidth);
            int height = (int) (this.requestedSector.deltaLatitude() / this.stitchSector.deltaLatitude() * completeStitchHeight);

            int[] completePixels = new int[width * height];
            COMPOSITE.getPixels(completePixels, 0, width, xStart, yStart, width, height);

            Bitmap sample = Bitmap.createBitmap(completePixels, width, height, Bitmap.Config.ARGB_8888);

            Bitmap resizedBitmap = Bitmap.createScaledBitmap(sample, this.desiredImageSize, this.desiredImageSize, false);

            this.imageTile.setImageSource(ImageSource.fromBitmap(resizedBitmap));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void enlargeBitmapResource(int size) {
        COMPOSITE = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        PIXELS = new int[size * size];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TileRequest that = (TileRequest) o;

        if (desiredImageSize != that.desiredImageSize) return false;
        if (!requestedSector.equals(that.requestedSector)) return false;
        if (!tileMatrixSet.equals(that.tileMatrixSet)) return false;
        return level.equals(that.level);
    }

    @Override
    public int hashCode() {
        int result = requestedSector.hashCode();
        result = 31 * result + tileMatrixSet.hashCode();
        result = 31 * result + desiredImageSize;
        result = 31 * result + level.hashCode();
        return result;
    }
}
