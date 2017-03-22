/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.Tile;

public class DataTile extends Tile {

    protected DataSource dataSource;

    public DataTile(Sector sector, Level level, int row, int column) {
        super(sector, level, row, column);
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public void setDataSource(DataSource data) {
        this.dataSource = data;
    }
}
