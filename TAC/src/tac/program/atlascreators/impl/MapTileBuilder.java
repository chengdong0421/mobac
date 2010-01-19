package tac.program.atlascreators.impl;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.openstreetmap.gui.jmapviewer.interfaces.MapSource;

import tac.exceptions.MapCreationException;
import tac.program.atlascreators.AtlasCreator;
import tac.program.atlascreators.tileprovider.TileProvider;
import tac.program.interfaces.MapInterface;
import tac.program.interfaces.TileImageDataWriter;
import tac.program.model.TileImageParameters;

public class MapTileBuilder {

	private static final Logger log = Logger.getLogger(MapTileBuilder.class);

	private final AtlasCreator atlasCreator;
	private final TileProvider mapDlTileProvider;
	private final MapInterface map;
	private final MapSource mapSource;
	private final TileImageParameters parameters;
	private final int tileSize;
	private final int xMin;
	private final int xMax;
	private final int yMin;
	private final int yMax;

	private int realWidth;
	private int realHeight;

	protected final MapTileWriter mapTileWriter;

	public MapTileBuilder(AtlasCreator atlasCreator, MapTileWriter mapTileWriter) {
		this.atlasCreator = atlasCreator;
		this.mapTileWriter = mapTileWriter;
		this.mapDlTileProvider = atlasCreator.getMapDlTileProvider();
		map = atlasCreator.getMap();
		mapSource = map.getMapSource();
		tileSize = mapSource.getMapSpace().getTileSize();
		xMax = atlasCreator.getXMax();
		xMin = atlasCreator.getXMin();
		yMax = atlasCreator.getYMax();
		yMin = atlasCreator.getYMin();
		parameters = atlasCreator.getParameters();

	}

	public void createTiles() throws MapCreationException, InterruptedException {
		// left upper point on the map in pixels
		// regarding the current zoom level
		int xStart = xMin * tileSize;
		int yStart = yMin * tileSize;

		// lower right point on the map in pixels
		// regarding the current zoom level
		int xEnd = xMax * tileSize + (tileSize - 1);
		int yEnd = yMax * tileSize + (tileSize - 1);

		int mergedWidth = xEnd - xStart + 1;
		int mergedHeight = yEnd - yStart + 1;

		// Reduce tile size of overall map height/width is smaller that one tile
		realWidth = parameters.getWidth();
		realHeight = parameters.getHeight();
		if (realWidth > mergedWidth)
			realWidth = mergedWidth;
		if (realHeight > mergedHeight)
			realHeight = mergedHeight;

		// Absolute positions
		int xAbsPos = xStart;
		int yAbsPos = yStart;

		log.trace("tile size: " + realWidth + " * " + realHeight);
		log.trace("X: from " + xStart + " to " + xEnd);
		log.trace("Y: from " + yStart + " to " + yEnd);

		// We don't work with large images, therefore we can disable the (file)
		// cache of ImageIO. This will speed up the creation process a bit
		ImageIO.setUseCache(false);
		ByteArrayOutputStream buf = new ByteArrayOutputStream(32768);
		TileImageDataWriter tileImageDataWriter = parameters.getFormat().getDataWriter();
		tileImageDataWriter.initialize();
		try {
			String tileType = tileImageDataWriter.getFileExt();
			int tiley = 0;
			while (yAbsPos < yEnd) {
				int tilex = 0;
				xAbsPos = xStart;
				while (xAbsPos < xEnd) {
					atlasCreator.checkUserAbort();
					atlasCreator.getAtlasProgress().incMapCreationProgress();
					BufferedImage tileImage = new BufferedImage(realWidth, realHeight,
							BufferedImage.TYPE_3BYTE_BGR);
					buf.reset();
					try {
						Graphics2D graphics = tileImage.createGraphics();
						graphics.setColor(mapSource.getBackgroundColor());
						graphics.fillRect(0, 0, realWidth, realHeight);
						paintCustomTile(graphics, xAbsPos, yAbsPos);
						graphics.dispose();
						tileImageDataWriter.processImage(tileImage, buf);
						mapTileWriter.writeTile(tilex, tiley, tileType, buf.toByteArray());
					} catch (IOException e) {
						throw new MapCreationException("Error writing tile image: "
								+ e.getMessage(), e);
					}

					tilex++;
					xAbsPos += realWidth;
				}
				tiley++;
				yAbsPos += realHeight;
			}
		} finally {
			tileImageDataWriter.dispose();
		}
	}

	/**
	 * Paints the graphics of the custom tile specified by the pixel coordinates
	 * <code>xAbsPos</code> and <code>yAbsPos</code> on the currently selected
	 * map & layer.
	 * 
	 * @param graphics
	 * @param xAbsPos
	 * @param yAbsPos
	 */
	private void paintCustomTile(Graphics2D graphics, int xAbsPos, int yAbsPos) {
		int xTile = xAbsPos / tileSize;
		int xTileOffset = -(xAbsPos % tileSize);

		for (int x = xTileOffset; x < realWidth; x += tileSize) {
			int yTile = yAbsPos / tileSize;
			int yTileOffset = -(yAbsPos % tileSize);
			for (int y = yTileOffset; y < realHeight; y += tileSize) {
				try {
					BufferedImage orgTileImage = loadOriginalMapTile(xTile, yTile);
					if (orgTileImage != null) {
						int w = orgTileImage.getWidth();
						int h = orgTileImage.getHeight();
						graphics.drawImage(orgTileImage, xTileOffset, yTileOffset, w, h, null);
					}
				} catch (Exception e) {
					log.error("Error while painting sub-tile", e);
				}
				yTile++;
				yTileOffset += tileSize;
			}
			xTile++;
			xTileOffset += tileSize;
		}
	}

	private BufferedImage loadOriginalMapTile(int xTile, int yTile) throws Exception {
		// log.trace("cache miss");
		BufferedImage image = mapDlTileProvider.getTileImage(xTile, yTile);
		return image;
	}

}
