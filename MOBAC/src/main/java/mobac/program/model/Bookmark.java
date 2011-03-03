/*******************************************************************************
 * Copyright (c) MOBAC developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package mobac.program.model;

import javax.xml.bind.annotation.XmlAttribute;

import mobac.mapsources.MapSourcesManager;
import mobac.program.interfaces.MapSource;

public class Bookmark extends EastNorthCoordinate {

	@XmlAttribute
	public int zoom;

	@XmlAttribute
	public String mapSource;

	/**
	 * Needed for JAXB
	 */
	@SuppressWarnings("unused")
	private Bookmark() {

	}

	public Bookmark(MapSource mapSource, int zoom, int pixelCoordinateX, int pixelCoordinateY) {
		super(mapSource.getMapSpace(), zoom, pixelCoordinateX, pixelCoordinateY);
		this.mapSource = mapSource.getName();
		this.zoom = zoom;
	}

	public MapSource getMapSource() {
		return MapSourcesManager.getInstance().getSourceByName(mapSource);
	}

	@Override
	public String toString() {
		return String.format("%s at lat=%.3f lon=%.3f (zoom = %d)", mapSource, lat, lon, zoom);
	}

}