package tac.program.mapcreators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import tac.exceptions.MapCreationException;
import tac.program.interfaces.MapInterface;
import tac.tar.TarIndex;
import tac.utilities.Utilities;

/**
 * Creates maps using the AndNav atlas format.
 * 
 * Please note that this atlas format ignores the defined atlas structure. It
 * uses a separate directory for each used map source and inside one directory
 * for each zoom level.
 */
public class MapCreatorAndNav extends MapCreator {

	private File mapZoomDir;

	protected String additionalFileExt = ".andnav";

	protected String tileType;

	public MapCreatorAndNav(MapInterface map, TarIndex tarTileIndex, File atlasDir) {
		super(map, tarTileIndex, atlasDir);
		File mapDir = new File(atlasDir, map.getMapSource().getName());
		mapZoomDir = new File(mapDir, Integer.toString(map.getZoom()));
		tileType = mapSource.getTileType();
		if (parameters != null) {
			mapDlTileProvider = new ConvertedRawTileProvider(mapDlTileProvider, parameters
					.getFormat());
			tileType = parameters.getFormat().getDataWriter().getFileExt();
		}
	}

	public void createMap() throws MapCreationException {
		try {
			Utilities.mkDirs(mapZoomDir);
		} catch (IOException e) {
			throw new MapCreationException(e);
		}

		// This means there should not be any resizing of the tiles.
		try {
			mapTileWriter = new AndNavTileWriter();
			createTiles();
		} catch (InterruptedException e) {
			// User has aborted process
			return;
		}
	}

	@Override
	protected void createTiles() throws InterruptedException, MapCreationException {
		atlasProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin + 1));
		ImageIO.setUseCache(false);

		for (int x = xMin; x <= xMax; x++) {
			File xDir = new File(mapZoomDir, Integer.toString(x));
			try {
				Utilities.mkDir(xDir);
			} catch (IOException e1) {
				throw new MapCreationException(e1);
			}
			for (int y = yMin; y <= yMax; y++) {
				checkUserAbort();
				atlasProgress.incMapCreationProgress();
				try {
					String tileFileName = x + "/" + y + "." + tileType + additionalFileExt;
					byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
					if (sourceTileData != null)
						mapTileWriter.writeTile(tileFileName, sourceTileData);
				} catch (IOException e) {
					log.error("", e);
				}
			}
		}
	}

	protected class AndNavTileWriter implements MapTileWriter {

		public void writeTile(String tileFileName, byte[] tileData) throws IOException {
			File f = new File(mapZoomDir, tileFileName);
			FileOutputStream out = new FileOutputStream(f);
			try {
				out.write(tileData);
			} finally {
				Utilities.closeStream(out);
			}
		}

		public void finalizeMap() {
		}
	}

}