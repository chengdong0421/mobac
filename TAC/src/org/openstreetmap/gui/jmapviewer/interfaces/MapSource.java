package org.openstreetmap.gui.jmapviewer.interfaces;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.openstreetmap.gui.jmapviewer.JMapViewer;

import tac.mapsources.MultiLayerMapSource;
import tac.program.jaxb.MapSourceAdapter;

//License: GPL. Copyright 2008 by Jan Peter Stotz

/**
 * 
 * @author Jan Peter Stotz
 */
@XmlJavaTypeAdapter(MapSourceAdapter.class)
public interface MapSource {

	/**
	 * Specifies the different mechanisms for detecting updated tiles
	 * respectively only download newer tiles than those stored locally.
	 * 
	 * <ul>
	 * <li>{@link #IfNoneMatch} Server provides ETag header entry for all tiles
	 * and <b>supports</b> conditional download via <code>If-None-Match</code>
	 * header entry.</li>
	 * <li>{@link #ETag} Server provides ETag header entry for all tiles but
	 * <b>does not support</b> conditional download via
	 * <code>If-None-Match</code> header entry.</li>
	 * <li>{@link #IfModifiedSince} Server provides Last-Modified header entry
	 * for all tiles and <b>supports</b> conditional download via
	 * <code>If-Modified-Since</code> header entry.</li>
	 * <li>{@link #LastModified} Server provides Last-Modified header entry for
	 * all tiles but <b>does not support</b> conditional download via
	 * <code>If-Modified-Since</code> header entry.</li>
	 * <li>{@link #None} The server does not support any of the listed
	 * mechanisms.</li>
	 * </ul>
	 * 
	 */
	public enum TileUpdate {
		IfNoneMatch, ETag, IfModifiedSince, LastModified, None
	}

	/**
	 * Specifies the maximum zoom value. The number of zoom levels is [0..
	 * {@link #getMaxZoom()}].
	 * 
	 * @return maximum zoom value that has to be smaller or equal to
	 *         {@link JMapViewer#MAX_ZOOM}
	 */
	public int getMaxZoom();

	/**
	 * Specifies the minimum zoom value. This value is usually 0. Only for maps
	 * that cover a certain region up to a limited zoom level this method should
	 * return a value different than 0.
	 * 
	 * @return minimum zoom value - usually 0
	 */
	public int getMinZoom();

	/**
	 * @return The supported tile update mechanism
	 * @see TileUpdate
	 */
	public TileUpdate getTileUpdate();

	/**
	 * A tile layer name has to be unique and has to consist only of characters
	 * valid for filenames.
	 * 
	 * @return Name of the tile layer
	 */
	public String getName();

	/**
	 * Constructs the tile url connection. If necessary the url connection can
	 * be prepared with cookies or other http specific headers which are
	 * required by the http server.
	 * 
	 * @param zoom
	 * @param tilex
	 *            tile number on x-axis for the specified <code>zoom</code>
	 *            level
	 * @param tiley
	 *            tile number on y-axis for the specified <code>zoom</code>
	 *            level
	 * @return the initialized urlConnection for downloading the specified tile
	 *         image
	 */
	public HttpURLConnection getTileUrlConnection(int zoom, int tilex, int tiley)
			throws IOException;

	/**
	 * Specifies the tile image type. For tiles rendered by Mapnik or
	 * Osmarenderer this is usually <code>"png"</code>.
	 * 
	 * @return file extension of the tile image type
	 */
	public String getTileType();

	/**
	 * Usually this method shoud return <code>true</code> but for debugging
	 * purposes it can be sometimes useful to deactivate local storing of tiles.
	 */
	public boolean allowFileStore();

	public MapSpace getMapSpace();

	/**
	 * Returns the tile store name. Usually this is identically to
	 * {@link #getName()} - only for {@link MultiLayerMapSource} this is usually
	 * a different name as the name refers to both layers and the store name
	 * only to the current overlay.
	 * 
	 * @return store name used for identifying the tile store on disk (offline
	 *         cache)
	 */
	public String getStoreName();
}
