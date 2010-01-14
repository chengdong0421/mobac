/* *********************************************
 * Copyright: Andreas Sander
 *
 *
 * ********************************************* */

package tac.program.atlascreators.impl.rmp;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import tac.gui.AtlasProgress;
import tac.program.atlascreators.impl.rmp.interfaces.RmpFileEntry;

/**
 * Class for building a TLM file from image and writing the file to a stream
 * 
 */
public class RmpLayer {
	private static final Logger log = Logger.getLogger(RmpLayer.class);

	private List<Tiledata> tiles;
	private TLMEntry tlmFile = null;

	private final AtlasProgress atlasProgress;

	/**
	 * Constructor
	 */
	public RmpLayer(AtlasProgress atlasProgress) {
		tiles = new LinkedList<Tiledata>();
		this.atlasProgress = atlasProgress;
	}

	/**
	 * Return the content of the A00 file as byte array
	 */
	public RmpFileEntry getA00File(String image_name) {
		return new A00Entry(image_name);
	}

	/**
	 * Return the content of the TLM file as byte array
	 * 
	 * @throws IOException
	 */
	public TLMEntry getTLMFile(String image_name) {
		tlmFile.setImageName(image_name);
		return tlmFile;
	}

	public void addPreparedImage(Tiledata tileData) throws IOException {
		tiles.add(tileData);
	}

	/**
	 * distribute the tiles over containers of max 256 tiles
	 */
	private TileContainer buildTileTree() {
		TileContainer[] container;
		TileContainer indexContainer = null;
		int containerCount;

		/*
		 * --- Calculate the number of tiles and tiles per container. 99 would
		 * be possible but we limit ourselves to 80 - That's enough ---
		 */
		int count = tiles.size();
		containerCount = count / 80;
		if (count % 80 != 0)
			containerCount++;

		int tilesPerContainer = count / containerCount;

		/* --- Create containers --- */
		container = new TileContainer[containerCount];
		for (int i = 0; i < containerCount; i++)
			container[i] = new TileContainer();

		/*
		 * --- We need an index container if there is more than one container.
		 * Container 0 is the previous of the index container ---
		 */
		if (containerCount > 1)
			indexContainer = new TileContainer(container[0]);

		/* --- Place the tiles into the container --- */
		int tileCount = 0;
		int totalTileCount = 0;
		int containerNumber = 0;
		for (Tiledata tiledata : tiles) {
			/*
			 * --- Starting with the second container, the first element is
			 * moved to the index container ---
			 */
			if (tileCount == 0 && containerNumber != 0)
				indexContainer.addTile(tiledata, container[containerNumber]);
			else
				container[containerNumber].addTile(tiledata, null);

			/* --- Switch to next container if we reach end of container --- */
			tileCount++;
			if (tileCount == tilesPerContainer) {
				containerNumber++;
				tileCount = 0;

				/*
				 * --- Recalculate the number of tiles per container because of
				 * rounding issues
				 */
				if (containerCount != containerNumber)
					tilesPerContainer = (count - (totalTileCount + 1))
							/ (containerCount - containerNumber);
			}
			totalTileCount++;
		}

		/*
		 * --- If we have multiple containers, then the index container is the
		 * result, otherwise the single container.
		 */
		if (indexContainer == null)
			return container[0];
		else
			return indexContainer;
	}

	/**
	 * Create the TLM file from the TileContainer infos
	 * 
	 * @throws IOException
	 */
	public void buildTLMFile(double tile_width, double tile_height, double left, double right,
			double top, double bottom) throws IOException {
		tlmFile = new TLMEntry(tile_width, tile_height, left, right, top, bottom);
		tlmFile.updateContent();
	}

	public class TLMEntry implements RmpFileEntry {

		private byte[] data = null;
		String imageName;
		final double tile_width;
		final double tile_height;
		final double left;
		final double right;
		final double top;
		final double bottom;

		public TLMEntry(double tile_width, double tile_height, double left, double right,
				double top, double bottom) {
			super();
			this.tile_width = tile_width;
			this.tile_height = tile_height;
			this.left = left;
			this.right = right;
			this.top = top;
			this.bottom = bottom;
		}

		public void updateContent() throws IOException {
			// calculate offset of each tile in A00 file
			int totaloffset = 4;
			for (Tiledata tile : tiles) {
				tile.totalOffset = totaloffset;
				totaloffset += tile.getTileDataSize() + 4;
			}

			/* --- Build the tile container --- */
			TileContainer container = buildTileTree();
			log.debug("Number of tiles: " + container.getTileCount());

			/* --- Create Output file --- */
			ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);

			/* --- header --- */
			RmpTools.writeValue(bos, 1, 4); // Start of block
			RmpTools.writeValue(bos, container.getTileCount(), 4); // Number of
			// tiles in files
			RmpTools.writeValue(bos, 256, 2); // Hor. size of tile in pixel
			RmpTools.writeValue(bos, 256, 2); // Vert. size of tile in pixel
			RmpTools.writeValue(bos, 1, 4); // Start of block
			RmpTools.writeDouble(bos, tile_height); // Height of tile in degree
			RmpTools.writeDouble(bos, tile_width); // Tile width in degree
			RmpTools.writeDouble(bos, left); // Frame
			RmpTools.writeDouble(bos, top); // Frame
			RmpTools.writeDouble(bos, right); // Frame
			RmpTools.writeDouble(bos, bottom); // Frame

			RmpTools.writeValue(bos, 0, 88); // Filler

			RmpTools.writeValue(bos, 256, 2); // Tile size ????
			RmpTools.writeValue(bos, 0, 2); // Filler

			int size = 256 + 1940 + 3 * 1992;
			size += container.getContainerCount() * 1992;
			if (container.getContainerCount() != 1)
				size += 1992;
			RmpTools.writeValue(bos, size, 4); // File size

			RmpTools.writeValue(bos, 0, 96); // Filler

			RmpTools.writeValue(bos, 1, 4); // Start of block
			RmpTools.writeValue(bos, 99, 4); // Number of tiles in block
			int firstBlockOffset = 0x0f5c + ((container.getContainerCount() == 1) ? 0 : 1992);
			RmpTools.writeValue(bos, firstBlockOffset, 4); // offset for first
			// block

			RmpTools.writeValue(bos, 0, 3920); // Filler

			/* --- Write the Tiledata --- */
			container.writeTree(bos);

			/* --- Add two empty blocks --- */
			RmpTools.writeValue(bos, 0, 1992 * 2);
			data = bos.toByteArray();
		}

		public void setImageName(String imageName) {
			this.imageName = imageName;
		}

		public String getFileExtension() {
			return "tlm";
		}

		public String getFileName() {
			return imageName;
		}

		public void writeFileContent(OutputStream out) throws IOException {
			out.write(data);
		}

	}

	protected class A00Entry implements RmpFileEntry {

		protected String name;

		public A00Entry(String name) {
			super();
			this.name = name;
		}

		public String getFileExtension() {
			return "a00";
		}

		public String getFileName() {
			return name;
		}

		public void writeFileContent(OutputStream os) throws IOException, InterruptedException {
			BufferedOutputStream bos = new BufferedOutputStream(os, 32768);
			/* --- Number of tiles --- */
			RmpTools.writeValue(bos, tiles.size(), 4);

			/* --- The tiles --- */
			int x = 0;
			int xMax = tiles.size();
			for (Tiledata tile : tiles) {
				tile.writeTileData(bos);
				if (Thread.currentThread().isInterrupted())
					throw new InterruptedException();

				atlasProgress.setMapCreationProgress((1000 * x++ / xMax));
			}
			bos.flush();
		}

		@Override
		public String toString() {
			return "A00Entry";
		}

	}
}
