/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wmts;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.ImageTile;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileFactory;

public class CompositorTileFactory implements TileFactory {

    protected WmtsTileMatrixSet tileMatrixSet;

    protected WmtsLayer layer;

    protected String template;

    public CompositorTileFactory(WmtsLayer layer, WmtsTileMatrixSet tileMatrixSet, String template) {
        this.layer = layer;
        this.tileMatrixSet = tileMatrixSet;
        this.template = template;
    }

    @Override
    public Tile createTile(Sector sector, Level level, int row, int column) {
        ImageTile imageTile = new ImageTile(sector, level, row, column);
        TileCompositor.getInstance().requestTile(sector, level, this.layer, this.tileMatrixSet, this.template, imageTile);
        return imageTile;
    }
}
