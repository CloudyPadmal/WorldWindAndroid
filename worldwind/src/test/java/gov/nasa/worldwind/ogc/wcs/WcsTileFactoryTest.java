/*
 * Copyright (c) 2017 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration. All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.wcs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logger;

import static junit.framework.Assert.assertEquals;

@RunWith(PowerMockRunner.class) // Support for mocking static methods
@PrepareForTest(Logger.class)   // We mock the Logger class to avoid its calls to android.util.log
public class WcsTileFactoryTest {

    @Test
    public void testUrlForTile_V100() throws Exception {
        String serviceAddress = "http://192.168.11.38:8080/geoserver/ows?";
        String version = "1.0.0";
        String coverageId = "world.topo.bathy";
        String format = "image/tiff";
        WcsTileFactory tileFactory = new WcsTileFactory(serviceAddress, version, coverageId, format);
        double minLat = 35.9;
        double minLon = -100.0;
        double maxLat = 37.0;
        double maxLon = -99.0;
        Sector sector = PowerMockito.mock(Sector.class);
        PowerMockito.when(sector.minLatitude()).thenReturn(minLat);
        PowerMockito.when(sector.minLongitude()).thenReturn(minLon);
        PowerMockito.when(sector.maxLatitude()).thenReturn(maxLat);
        PowerMockito.when(sector.maxLongitude()).thenReturn(maxLon);
        String expectedUrl = "http://192.168.11.38:8080/geoserver/ows?SERVICE=WCS&VERSION=1.0.0&REQUEST=GetCoverage&FORMAT=image/tiff&COVERAGE=world.topo.bathy&CRS=EPSG:4326&BBOX=-100.0,35.9,-99.0,37.0&WIDTH=256&HEIGHT=256";

        String url = tileFactory.urlForTile(sector, 256, 256);

        assertEquals("version 1.0.0 url", expectedUrl, url);
    }

    @Test
    public void testUrlForTile_V201() throws Exception {
        String serviceAddress = "http://192.168.11.38:8080/geoserver/ows?";
        String version = "2.0.1";
        String coverageId = "world.topo.bathy";
        String format = "image/tiff";
        String srsLatLabel = "Lat";
        String srsLonLabel = "Long";
        String gridLatLabel = "http://www.opengis.net/def/axis/OGC/1/i";
        String gridLonLabel = "http://www.opengis.net/def/axis/OGC/1/j";
        WcsTileFactory tileFactory = new WcsTileFactory(serviceAddress, version, coverageId, format, srsLatLabel,
            srsLonLabel, gridLatLabel, gridLonLabel);
        double minLat = 35.9;
        double minLon = -100.0;
        double maxLat = 37.0;
        double maxLon = -99.0;
        Sector sector = PowerMockito.mock(Sector.class);
        PowerMockito.when(sector.minLatitude()).thenReturn(minLat);
        PowerMockito.when(sector.minLongitude()).thenReturn(minLon);
        PowerMockito.when(sector.maxLatitude()).thenReturn(maxLat);
        PowerMockito.when(sector.maxLongitude()).thenReturn(maxLon);
        String expectedUrl = "http://192.168.11.38:8080/geoserver/ows?SERVICE=WCS&VERSION=2.0.1&REQUEST=GetCoverage&FORMAT=image/tiff&COVERAGEID=world.topo.bathy&SUBSET=Lat(35.9,37.0)&SUBSET=Long(-100.0,-99.0)&SCALESIZE=http://www.opengis.net/def/axis/OGC/1/i(256),http://www.opengis.net/def/axis/OGC/1/j(256)";

        String url = tileFactory.urlForTile(sector, 256, 256);

        assertEquals("version 2.0.1 url", expectedUrl, url);
    }
}
