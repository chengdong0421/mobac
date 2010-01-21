package tac.program;

import java.util.Locale;

import javax.swing.JOptionPane;

import org.apache.log4j.Logger;

import tac.mapsources.impl.Google;
import tac.program.model.Atlas;
import tac.program.model.EastNorthCoordinate;
import tac.program.model.Layer;
import tac.program.model.Profile;
import tac.program.model.Settings;
import tac.utilities.TACExceptionHandler;

/**
 * Creates the necessary files on first time Mobile Atlas Creator is started
 * or tries to update the environment if the version has changed.
 */
public class EnvironmentSetup {

	public static Logger log = Logger.getLogger(EnvironmentSetup.class);

	public static void checkMemory() {
		Runtime r = Runtime.getRuntime();
		long maxHeap = r.maxMemory();
		String heapMBFormatted = String.format(Locale.ENGLISH, "%3.2f MiB", maxHeap / 1048576d);
		log.info("Total avialable memory to TAC: " + heapMBFormatted);
		if (maxHeap < 100000000) {
			String msg = "<html><b>WARNING:</b> Mobile Atlas Creator has been started "
					+ "with a very small amount of memory assigned.<br>"
					+ "The current maximum usable amount of memory to Mobile Atlas Creator is <b>"
					+ heapMBFormatted
					+ "</b>.<br><br>Please make sure to start Mobile Atlas Creator in "
					+ "the future via the provided start scripts <i>Mobile Atlas Creator.exe</i><br>"
					+ "on Windows or <i>start.sh</i> on Linux/Unix/OSX or add the "
					+ "parameter <b>-Xmx 512M</b> to your startup command.<br><br>"
					+ "Example: <i>java -Xmx512M -jar Mobile_Atlas_Creator.jar</i><br>"
					+ "<br><center>Press OK to continue and start Mobile Atlas Creator</center></html>";
			JOptionPane.showMessageDialog(null, msg, "Warning: low memory",
					JOptionPane.WARNING_MESSAGE);
		}
	}

	public static void checkFileSetup() {
		if (Settings.FILE.exists())
			return;

		try {
			Settings.save();
		} catch (Exception e) {
			log.error("Error while creating settings.xml: " + e.getMessage(), e);
			String[] options = { "Exit", "Show error report" };
			int a = JOptionPane.showOptionDialog(null,
					"Could not create file settings.xml program will exit.", "Error", 0,
					JOptionPane.ERROR_MESSAGE, null, options, options[0]);
			if (a == 1)
				TACExceptionHandler.showExceptionDialog(e);
			System.exit(1);
		}
		Profile p = new Profile("Google Maps New York");
		Atlas atlas = Atlas.newInstance();
		try {
			EastNorthCoordinate max = new EastNorthCoordinate(40.97264, -74.142609);
			EastNorthCoordinate min = new EastNorthCoordinate(40.541982, -73.699036);
			Layer layer = new Layer(atlas, "GM New York");
			layer.addMapsAutocut("GM New York 16", new Google.GoogleMaps(), max, min, 16, null,
					32000);
			layer.addMapsAutocut("GM New York 14", new Google.GoogleMaps(), max, min, 14, null,
					32000);
			atlas.addLayer(layer);
			p.save(atlas);
		} catch (Exception e) {
			log.error("Creation for example profiles failed", e);
			TACExceptionHandler.showExceptionDialog(e);
		}
	}
}
