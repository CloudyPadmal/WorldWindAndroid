/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wcs;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DataSource;
import gov.nasa.worldwind.render.DataTile;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileFactory;

/**
 * Simple WCS Tile Factory which can communicate with WCS servers using either version 1.0.0 or 2.0.1 of the WCS
 * specification.
 */
public class WcsTileFactory implements TileFactory {

    protected String serviceAddress;

    protected String version;

    protected String coverageId;

    protected String format;

    /**
     * The srs axis label for the latitude.
     */
    protected String srsLatAxisLabel;

    /**
     * The srs axis label for the longitude.
     */
    protected String srsLonAxisLabel;

    /**
     * The grid axis label for the latitude axis. When using GeoServer or the World Wind Server Kit the full URI must
     * be specified, e.g. http://www.opengis.net/def/axis/OGC/1/ + gridAxisLabel
     */
    protected String gridLatAxisLabel;

    /**
     * The grid axis label for the longitude axis. When using GeoServer or the World Wind Server Kit the full URI must
     * be specified, e.g. http://www.opengis.net/def/axis/OGC/1/ + gridAxisLabel
     */
    protected String gridLonAxisLabel;

    protected StringBuilder url;

    /**
     * Constructor for WCS servers using the 1.0.0 specification. The CRS system is defined as EPSG:4326.
     *
     * @param serviceAddress
     * @param version
     * @param coverageId
     * @param format
     */
    public WcsTileFactory(String serviceAddress, String version, String coverageId, String format) {
        this.serviceAddress = serviceAddress;
        this.version = version;
        this.coverageId = coverageId;
        this.format = format;
    }

    /**
     * Constructor for WCS servers using the 2.0.1 specification.
     *
     * @param serviceAddress
     * @param version
     * @param coverageId
     * @param format
     * @param srsLatAxisLabel
     * @param srsLonAxisLabel
     * @param gridLatAxisLabel
     * @param gridLonAxisLabel
     */
    public WcsTileFactory(String serviceAddress, String version, String coverageId, String format,
                          String srsLatAxisLabel, String srsLonAxisLabel, String gridLatAxisLabel,
                          String gridLonAxisLabel) {
        this.serviceAddress = serviceAddress;
        this.version = version;
        this.coverageId = coverageId;
        this.format = format;
        this.srsLatAxisLabel = srsLatAxisLabel;
        this.srsLonAxisLabel = srsLonAxisLabel;
        this.gridLatAxisLabel = gridLatAxisLabel;
        this.gridLonAxisLabel = gridLonAxisLabel;
    }

    @Override
    public Tile createTile(Sector sector, Level level, int row, int column) {
        DataTile tile = new DataTile(sector, level, row, column);

        String urlString = this.urlForTile(sector, level.tileWidth, level.tileHeight);
        if (urlString != null) {
            tile.setDataSource(DataSource.fromUrl(urlString));
            return tile;
        } else {
            return null;
        }
    }

    protected String urlForTile(Sector sector, int tileWidth, int tileHeight) {
        this.url = new StringBuilder();

        this.url.append(this.serviceAddress);

        int index = url.indexOf("?");
        if (index < 0) { // if service address contains no query delimiter
            url.append("?"); // add one
        } else if (index != url.length() - 1) { // else if query delimiter not at end of string
            index = url.lastIndexOf("&");
            if (index != url.length() - 1) {
                url.append("&"); // add a parameter delimiter
            }
        }

        this.url.append("SERVICE=WCS");
        this.url.append("&VERSION=").append(this.version);
        this.url.append("&REQUEST=GetCoverage");
        this.url.append("&FORMAT=").append(this.format);

        if (this.version.equals("1.0.0")) {
            this.url.append("&COVERAGE=").append(this.coverageId);
            this.url.append("&CRS=EPSG:4326");
            this.url.append("&BBOX=").append(sector.minLongitude()).append(",").append(sector.minLatitude()).append(",").append(sector.maxLongitude()).append(",").append(sector.maxLatitude());
            this.url.append("&WIDTH=").append(tileWidth);
            this.url.append("&HEIGHT=").append(tileHeight);
        } else if (this.version.equals("2.0.1")) {
            this.url.append("&COVERAGEID=").append(this.coverageId);
            this.url.append("&SUBSET=").append(this.srsLatAxisLabel).append("(").append(sector.minLatitude()).append(",").append(sector.maxLatitude()).append(")");
            this.url.append("&SUBSET=").append(this.srsLonAxisLabel).append("(").append(sector.minLongitude()).append(",").append(sector.maxLongitude()).append(")");
            this.url.append("&SCALESIZE=").append(this.gridLatAxisLabel).append("(").append(tileHeight).append(")").append(",").append(this.gridLonAxisLabel).append("(").append(tileWidth).append(")");
        } else {
            return null;
        }

        return this.url.toString();
    }
}
