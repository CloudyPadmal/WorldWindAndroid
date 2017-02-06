/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wmts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ImageRetriever;
import gov.nasa.worldwind.render.ImageTile;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Logger;
import gov.nasa.worldwind.util.WWMath;

public class TileCompositor {

    protected static TileCompositor INSTANCE;

    protected ExecutorService service = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "gov.nasa.worldwind.tilecompositor");
        }
    });

    protected Set<gov.nasa.worldwind.ogc.wmts.TileRequest> pendingTileRequests = new HashSet<>();

    protected Object ptrLock = new Object();

    protected Map<String, TileImageRequest> pendingImageRequests = new HashMap<>();

    protected Object ptirLock = new Object();

    protected ImageRetriever imageRetriever = new ImageRetriever(4);

    protected TileCompositor() {

    }

    public synchronized static TileCompositor getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TileCompositor();
        }
        return INSTANCE;
    }

    public void requestTile(Sector sector, Level level, WmtsLayer layer, WmtsTileMatrixSet tileMatrixSet, String template, ImageTile imageTile) {
        // TODO Tile Matrix Set Bounding Box Sector
        // TODO url template
        // TODO Tile Matrix to Top Left Corner Position Map

        // First try the layer WGS84 Bounding Box
        Sector tileMatrixSetBoundingBox = (layer.getWgs84BoundingBox() != null) ? layer.getWgs84BoundingBox().getSector() : null;

//        // Next try the other bounding boxes described by the layer
//        List<OwsBoundingBox> otherLayerBoundingBoxes = layer.getBoundingBoxes();
//        for (OwsBoundingBox owsBoundingBox : otherLayerBoundingBoxes) {
//            if (owsBoundingBox.getCrs().equals(tileMatrixSet.getSupportedCrs())) {
//                String upperCorner = owsBoundingBox.getUpperCorner();
//                String lowerCorner = owsBoundingBox.getLowerCorner();
//                String crs = owsBoundingBox.getCrs();
//            }
//        }

        Map<WmtsTileMatrix, Position> topLeftCornerMap = new HashMap<>();
        double[] vals = new double[2];
        for (WmtsTileMatrix tileMatrix : tileMatrixSet.getTileMatrices()) {

            this.parseValues(tileMatrix.getTopLeftCorner(), vals);
            this.epsg3857ToEpsg4326(vals[0], vals[1], vals);
            Position pos = new Position(vals[0], vals[1], 0.0);
            topLeftCornerMap.put(tileMatrix, pos);
        }

        TileRequest tileRequest = new TileRequest(sector, tileMatrixSet, tileMatrixSetBoundingBox, imageTile, 256, level, template, topLeftCornerMap);
        this.service.submit(tileRequest);
    }

    //
//    public static class TileRequest implements Runnable {
//
//        protected Sector tileMatrixSetSector;
//
//        protected Sector sector;
//
//        protected Level level;
//
//        protected WmtsTileMatrixSet tileMatrixSet;
//
//        protected String template;
//
//        protected ImageTile imageTile;
//
//        protected ImageRequest[] imageRequests;
//
//        protected AtomicInteger completeImageRequestCount = new AtomicInteger();
//
//        public TileRequest(Sector sector, Level level, WmtsTileMatrixSet tileMatrixSet, String template, ImageTile imageTile) {
//            this.sector = sector;
//            this.level = level;
//            this.convertTileMatrixSet(tileMatrixSet);
//            this.template = template;
//            this.imageTile = imageTile;
//        }
//
//        @Override
//        public void run() {
//            if (this.imageRequests == null) {
//                this.buildTiles();
//            } else {
//                this.done();
//            }
//        }
//
//        protected void buildTiles() {
//            WmtsTileMatrix suitableTileMatrix = this.determineSuitableTileMatrix(this.level);
//            this.determineTileIndices(suitableTileMatrix);
//        }
//
//        protected void determineTileIndices(WmtsTileMatrix tileMatrix) {
//
//            double deltaX = this.tileMatrixSetSector.deltaLongitude();
//            double deltaY = this.tileMatrixSetSector.deltaLatitude();
//
//            int tileMinRow = (int) ((this.sector.minLatitude() + 90.0) / deltaY);
//            int tileMaxRow = (int) ((this.sector.maxLatitude() + 90.0) / deltaY);
//            int tileMinCol = (int) ((this.sector.minLongitude() + 90.0) / deltaX);
//            int tileMaxCol = (int) ((this.sector.maxLongitude() + 90.0) / deltaX);
//
//            this.imageRequests = new ImageRequest[(tileMaxRow - tileMinRow + 1) * (tileMaxCol - tileMinCol + 1)];
//            synchronized (TileCompositor.getInstance().pirLock) {
//                for (int i = tileMinRow; i <= tileMaxRow; i++) {
//                    for (int j = tileMinCol; j <= tileMaxCol; j++) {
//
//                        String url = this.urlForTile(i, j, tileMatrix.getIdentifier());
//                        ImageRequest imageRequest = new ImageRequest(url, this.tileMatrixSet, i, j, this.level.levelNumber);
//
//                        if (TileCompositor.getInstance().pendingImageRequests.containsKey(imageRequest)) {
//                            imageRequest = TileCompositor.getInstance().pendingImageRequests.get(url);
//                            imageRequest.addRequestListener(this);
//                        } else {
//                            imageRequest.addRequestListener(this);
//                            TileCompositor.getInstance().pendingImageRequests.put(url, imageRequest);
//                            ImageSource imageSource = ImageSource.fromUrl(url);
//                            TileCompositor.getInstance().imageRetriever.retrieve(imageSource, null, imageRequest);
//                        }
//
//                    }
//                }
//            }
//        }
//
//        public String urlForTile(int row, int column, String tileMatrixIdentifier) {
//
//            String url = this.template.replace(TILEMATRIX_TEMPLATE, tileMatrixIdentifier);
//            url = url.replace(TILEROW_TEMPLATE, row + "");
//            url = url.replace(TILECOL_TEMPLATE, column + "");
//
//            return url;
//        }
//
//        protected WmtsTileMatrix determineSuitableTileMatrix(Level level) {
//            // determine suitable tile matrix given scale denominator
//            for (int i = 0; i < this.tileMatrixSet.getTileMatrices().size(); i++) {
//                WmtsTileMatrix tileMatrix = this.tileMatrixSet.getTileMatrices().get(i);
//                LEVEL_SET_CONFIG.tileWidth = tileMatrix.getTileWidth();
//                LEVEL_SET_CONFIG.tileHeight = tileMatrix.getTileHeight();
//                double minMetersPerPixel = tileMatrix.getScaleDenominator() * 0.00028;
//                double minRadiansPerPixel = minMetersPerPixel / WorldWind.WGS84_SEMI_MAJOR_AXIS;
//                int scaleLevel = LEVEL_SET_CONFIG.numLevelsForResolution(minRadiansPerPixel) - 1;
//                if (scaleLevel > level.levelNumber) {
//                    return this.tileMatrixSet.getTileMatrices().get(Math.min(0, i - 1));
//                }
//            }
//
//            return null;
//        }
//
//        protected void convertTileMatrixSet(WmtsTileMatrixSet tileMatrixSet) {
//            // put everything into EPSG:4326
//            if (tileMatrixSet.getSupportedCrs().contains("3857")) {
//                this.tileMatrixSet = new WmtsTileMatrixSet();
//                this.tileMatrixSet.identifier = tileMatrixSet.getIdentifier();
//                // convert the bounding box
//                OwsBoundingBox convertedBoundingBox = new OwsBoundingBox();
//                OwsBoundingBox boundingBox = tileMatrixSet.getBoundingBox();
//                this.parseValues(boundingBox.getLowerCorner(), SCRATCH_ONE);
//                this.epsg3857ToEpsg4326(SCRATCH_ONE[0], SCRATCH_ONE[1], SCRATCH_ONE);
//                this.parseValues(boundingBox.getUpperCorner(), SCRATCH_TWO);
//                this.epsg3857ToEpsg4326(SCRATCH_TWO[0], SCRATCH_TWO[1], SCRATCH_TWO);
//                convertedBoundingBox.lowerCorner = SCRATCH_ONE[0] + " " + SCRATCH_ONE[1];
//                convertedBoundingBox.upperCorner = SCRATCH_TWO[0] + " " + SCRATCH_TWO[1];
//                this.tileMatrixSet.boundingBox = convertedBoundingBox;
//                this.tileMatrixSetSector = Sector.fromDegrees(SCRATCH_ONE[0], SCRATCH_ONE[1], SCRATCH_TWO[0] - SCRATCH_ONE[0], SCRATCH_TWO[1] - SCRATCH_ONE[1]);
//                // step through each tile matrix
//                for (WmtsTileMatrix tileMatrix : tileMatrixSet.getTileMatrices()) {
//                    WmtsTileMatrix convertedTileMatrix = new WmtsTileMatrix();
//                    // copy the properties
//                    convertedTileMatrix.tileWidth = tileMatrix.tileWidth;
//                    convertedTileMatrix.tileHeight = tileMatrix.tileHeight;
//                    convertedTileMatrix.matrixWidth = tileMatrix.matrixWidth;
//                    convertedTileMatrix.matrixHeight = tileMatrix.matrixHeight;
//                    convertedTileMatrix.scaleDenominator = tileMatrix.scaleDenominator;
//                    convertedTileMatrix.identifier = tileMatrix.identifier;
//                    this.parseValues(tileMatrix.topLeftCorner, SCRATCH_ONE);
//                    this.epsg3857ToEpsg4326(SCRATCH_ONE[0], SCRATCH_ONE[1], SCRATCH_ONE);
//                    convertedTileMatrix.topLeftCorner = SCRATCH_ONE[0] + " " + SCRATCH_ONE[1];
//                    this.tileMatrixSet.tileMatrices.add(convertedTileMatrix);
//                }
//            } else {
//                this.tileMatrixSet = new WmtsTileMatrixSet();
//                this.tileMatrixSet.identifier = tileMatrixSet.getIdentifier();
//                if (tileMatrixSet.getBoundingBox() == null) {
//                    this.tileMatrixSetSector = new Sector().setFullSphere();
//                    this.tileMatrixSet.tileMatrices = tileMatrixSet.getTileMatrices();
//                } else {
//                    this.parseValues(tileMatrixSet.getBoundingBox().getLowerCorner(), SCRATCH_ONE);
//                    this.parseValues(tileMatrixSet.getBoundingBox().getUpperCorner(), SCRATCH_TWO);
//                    this.tileMatrixSet.boundingBox = new OwsBoundingBox();
//                    if (tileMatrixSet.getSupportedCrs().contains("4326")) {
//                        this.tileMatrixSetSector = Sector.fromDegrees(SCRATCH_ONE[0], SCRATCH_ONE[1], SCRATCH_TWO[0] - SCRATCH_ONE[0], SCRATCH_TWO[1] - SCRATCH_ONE[1]);
//                        this.tileMatrixSet.boundingBox.lowerCorner = SCRATCH_ONE[0] + " " + SCRATCH_ONE[1];
//                        this.tileMatrixSet.tileMatrices = tileMatrixSet.getTileMatrices();
//                    } else if (tileMatrixSet.getSupportedCrs().contains("84")) {
//                        this.tileMatrixSetSector = Sector.fromDegrees(SCRATCH_ONE[1], SCRATCH_ONE[0], SCRATCH_TWO[1] - SCRATCH_ONE[1], SCRATCH_TWO[0] - SCRATCH_ONE[0]);
//                        this.tileMatrixSet.boundingBox.lowerCorner = SCRATCH_ONE[1] + " " + SCRATCH_ONE[0];
//                        // iterate through and flip coordinates
//                        for (WmtsTileMatrix tileMatrix : tileMatrixSet.getTileMatrices()) {
//                            WmtsTileMatrix convertedTileMatrix = new WmtsTileMatrix();
//                            // copy the properties
//                            convertedTileMatrix.tileWidth = tileMatrix.tileWidth;
//                            convertedTileMatrix.tileHeight = tileMatrix.tileHeight;
//                            convertedTileMatrix.matrixWidth = tileMatrix.matrixWidth;
//                            convertedTileMatrix.matrixHeight = tileMatrix.matrixHeight;
//                            convertedTileMatrix.scaleDenominator = tileMatrix.scaleDenominator;
//                            convertedTileMatrix.identifier = tileMatrix.identifier;
//                            this.parseValues(tileMatrix.topLeftCorner, SCRATCH_ONE);
//                            convertedTileMatrix.topLeftCorner = SCRATCH_ONE[1] + " " + SCRATCH_ONE[0];
//                            this.tileMatrixSet.tileMatrices.add(convertedTileMatrix);
//                        }
//                    }
//                }
//            }
//
//        }
//
    protected void parseValues(String values, double[] result) {
        try {
            String[] vals = values.split("\\s+");
            result[0] = Double.parseDouble(vals[0]);
            result[1] = Double.parseDouble(vals[1]);
        } catch (Exception e) {
            Logger.logMessage(Logger.ERROR, "WmtsMatrixConversionTileFactory", "parseValues", "Unable to parse values", e);
        }
    }

    protected void epsg3857ToEpsg4326(double easting, double northing, double[] result) {
        double r = 6.3781e6;
        double latRadians = (Math.PI / 2) - 2 * Math.atan(Math.exp(-northing / r));
        double lonRadians = easting / r;

        result[0] = WWMath.clamp(Math.toDegrees(latRadians), -90.0, 90.0);
        result[1] = WWMath.clamp(Math.toDegrees(lonRadians), -180.0, 180.0);
    }
//
//        protected void imageRequestComplete(ImageRequest imageRequest) {
//            int index = this.completeImageRequestCount.getAndIncrement();
//            this.imageRequests[index] = imageRequest;
//            if (this.completeImageRequestCount.get() == this.imageRequests.length) {
//                TileCompositor.getInstance().service.submit(this);
//            }
//        }
//
//        protected void done() {
//
//            int minRow = Integer.MAX_VALUE;
//            int maxRow = -Integer.MAX_VALUE;
//            int minCol = Integer.MAX_VALUE;
//            int maxCol = -Integer.MAX_VALUE;
//
//            for (ImageRequest imageRequest : this.imageRequests) {
//                minRow = Math.min(minRow, imageRequest.row);
//                maxRow = Math.max(maxRow, imageRequest.row);
//                minCol = Math.min(minCol, imageRequest.col);
//                maxCol = Math.max(maxCol, imageRequest.col);
//            }
//
//            try {
//                Bitmap stitch = Bitmap.createBitmap((maxCol - minCol + 1) * 256, (maxRow - minRow + 1) * 256, Bitmap.Config.ARGB_8888);
//                int[] pixels = new int[256 * 256];
//                for (int i = 0; i < this.imageRequests.length; i++) {
//                    this.imageRequests[i].bitmap.getPixels(pixels, 0, 256, 0, 0, 256, 256);
//                    int rowOffset = (this.imageRequests[i].row - minRow) * 256;
//                    int colOffset = (this.imageRequests[i].col - minCol) * 256;
//                    stitch.setPixels(pixels, 0, 256, colOffset, rowOffset, 256, 256);
//                }
//
//                // need to sample our tile and then resize to desired dimensions
//                int x =
//
//                this.imageTile.setImageSource(ImageSource.fromBitmap(stitch));
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            TileRequest that = (TileRequest) o;
//
//            if (tileMatrixSetSector != null ? !tileMatrixSetSector.equals(that.tileMatrixSetSector) : that.tileMatrixSetSector != null) return false;
//            if (level != null ? !level.equals(that.level) : that.level != null) return false;
//            return tileMatrixSet != null ? tileMatrixSet.equals(that.tileMatrixSet) : that.tileMatrixSet == null;
//        }
//
//        @Override
//        public int hashCode() {
//            int result = tileMatrixSetSector != null ? tileMatrixSetSector.hashCode() : 0;
//            result = 31 * result + (level != null ? level.hashCode() : 0);
//            result = 31 * result + (tileMatrixSet != null ? tileMatrixSet.hashCode() : 0);
//            return result;
//        }
//    }
//
//    public static class ImageRequest implements Retriever.Callback {
//
//        protected String url;
//
//        //protected WmtsTileMatrixSet tileMatrixSet;
//
//        protected int row;
//
//        protected int col;
//
//        protected int level;
//
//        protected Set<TileRequest> tileRequestListeners = new LinkedHashSet<>();
//
//        protected Object listLock = new Object();
//
//        protected Bitmap bitmap;
//
//        public ImageRequest(String url, WmtsTileMatrixSet tileMatrixSet, int row, int col, int level) {
//            this.url = url;
//            this.tileMatrixSet = tileMatrixSet;
//            this.row = row;
//            this.col = col;
//            this.level = level;
//        }
//
//        protected void addRequestListener(TileRequest tileRequest) {
//            synchronized (this.listLock) {
//                tileRequestListeners.add(tileRequest);
//            }
//        }
//
//        @Override
//        public void retrievalSucceeded(Retriever retriever, Object key, Object options, Object value) {
//            this.bitmap = (Bitmap) value;
//            synchronized (TileCompositor.getInstance().pirLock) {
//                TileCompositor.getInstance().pendingImageRequests.remove(this);
//            }
//            synchronized (this.listLock) {
//                for (TileRequest  tileRequest : this.tileRequestListeners) {
//                    tileRequest.imageRequestComplete(this);
//                }
//            }
//        }
//
//        @Override
//        public void retrievalFailed(Retriever retriever, Object key, Throwable ex) {
//            Logger.logMessage(Logger.ERROR, "TileCompositor.ImageRequest", "retrievalFailed", "Image Retrieval Failed", ex);
//            synchronized (TileCompositor.getInstance().pirLock) {
//                TileCompositor.getInstance().pendingImageRequests.remove(this);
//            }
//        }
//
//        @Override
//        public void retrievalRejected(Retriever retriever, Object key) {
//            retriever.retrieve(TileCompositor.getInstance().imageRetriever, null, this);
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//
//            ImageRequest that = (ImageRequest) o;
//
//            return url != null ? url.equals(that.url) : that.url == null;
//        }
//
//        @Override
//        public int hashCode() {
//            return url != null ? url.hashCode() : 0;
//        }
//    }
}
