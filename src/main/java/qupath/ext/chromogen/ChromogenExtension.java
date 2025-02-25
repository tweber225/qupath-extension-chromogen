package qupath.ext.chromogen;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.chromogen.ui.InterfaceController;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.prefs.controlsfx.PropertyItemBuilder;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ResourceBundle;


/**
 * This is a demo to provide a template for creating a new QuPath extension.
 * <p>
 * It doesn't do much - it just shows how to add a menu item and a preference.
 * See the code and comments below for more info.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name &amp; package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
public class ChromogenExtension implements QuPathExtension, GitHubProject {
	/**
	 * A resource bundle containing all the text used by the extension. This may be useful for translation to other languages.
	 * Note that this is optional and you can define the text within the code and FXML files that you use.
	 */
	private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.chromogen.ui.strings");
	private static final Logger logger = LoggerFactory.getLogger(ChromogenExtension.class);

	/**
	 * Display name for your extension
	 */
	private static final String EXTENSION_NAME = resources.getString("name");

	/**
	 * Short description, used under 'Extensions > Installed extensions'
	 */
	private static final String EXTENSION_DESCRIPTION = resources.getString("description");

	/**
	 * QuPath version that the extension is designed to work with.
	 * This allows QuPath to inform the user if it seems to be incompatible.
	 */
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.5.1");

	/**
	 * GitHub repo that your extension can be found at.
	 * This makes it easier for users to find updates to your extension.
	 * If you don't want to support this feature, you can remove
	 * references to GitHubRepo and GitHubProject from your extension.
	 */
	private static final GitHubRepo EXTENSION_REPOSITORY = GitHubRepo.create(
			EXTENSION_NAME, "tweber225", "qupath-extension-chromogen");

	/**
	 * Flag whether the extension is already installed (might not be needed... but we'll do it anyway)
	 */
	private boolean isInstalled = false;

	/**
	 * A 'persistent preference' - showing how to create a property that is stored whenever QuPath is closed.
	 * This preference will be managed in the main QuPath GUI preferences window.
	 */
	private static final BooleanProperty enableExtensionProperty = PathPrefs.createPersistentPreference(
			"enableExtension", true);

	/**
	 * Another 'persistent preference'.
	 * This one will be managed using a GUI element created by the extension.
	 * We use {@link Property<Integer>} rather than {@link IntegerProperty}
	 * because of the type of GUI element we use to manage it.
	 */
	private static final Property<Integer> integerOption = PathPrefs.createPersistentPreference(
			"chromogen.num.option", 1).asObject();

	/**
	 * An example of how to expose persistent preferences to other classes in your extension.
	 * @return The persistent preference, so that it can be read or set somewhere else.
	 */
	public static Property<Integer> integerOptionProperty() {
		return integerOption;
	}

	/**
	 * Hold the QuPathGUI reference in a static field
	 */
	private static QuPathGUI qupathGUI;

	/**
	 * Create a stage for the extension to display
	 */
	private Stage stage;

	@Override
	public void installExtension(QuPathGUI qupath) {
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
			return;
		}
		if (qupathGUI == null) {
			qupathGUI = qupath;
		}
		
		isInstalled = true;
		addPreferenceToPane(qupath);
		addMenuItem(qupath);
	}

	/**
	 * Demo showing how to add a persistent preference to the QuPath preferences pane.
	 * The preference will be in a section of the preference pane based on the
	 * category you set. The description is used as a tooltip.
	 * @param qupath The currently running QuPathGUI instance.
	 */
	private void addPreferenceToPane(QuPathGUI qupath) {
        var propertyItem = new PropertyItemBuilder<>(enableExtensionProperty, Boolean.class)
				.name(resources.getString("menu.enable"))
				.category("Chromogen extension")
				.description("Enable the Chromogen extension")
				.build();
		qupath.getPreferencePane()
				.getPropertySheet()
				.getItems()
				.add(propertyItem);
	}


	/**
	 * Demo showing how a new command can be added to a QuPath menu.
	 * @param qupath The QuPath GUI
	 */
	private void addMenuItem(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
		MenuItem menuItem = new MenuItem("Chromogen");
		menuItem.setOnAction(e -> createStage());
		menuItem.disableProperty().bind(enableExtensionProperty.not());
		menu.getItems().add(menuItem);
	}

	/**
	 * Demo showing how to create a new stage with a JavaFX FXML interface.
	 */
	private void createStage() {
		if (stage == null) {
			try {
				stage = new Stage();
				Scene scene = new Scene(InterfaceController.createInstance());
				stage.initOwner(QuPathGUI.getInstance().getStage());
				stage.setTitle(resources.getString("stage.title"));
				stage.setScene(scene);
				stage.setResizable(false);
			} catch (IOException e) {
				Dialogs.showErrorMessage(resources.getString("error"), resources.getString("error.gui-loading-failed"));
				logger.error("Unable to load extension interface FXML", e);
			}
		}
		stage.show();
	}

	 /**
     * Example method that transforms the current image using your ChromogenImageServer.
     */
    public static void applyChromogenTransform() {
		logger.info("Applying Transform");
		if (qupathGUI == null) {
			Dialogs.showErrorMessage("Error", "QuPath GUI is not initialized!");
			return;
		}
		ImageData<?> imageData = qupathGUI.getImageData();
		if (imageData == null) {
			Dialogs.showErrorMessage("Error", "No image is currently open.");
			return;
		}
	
		var server = imageData.getServer();
		if (!(server instanceof ImageServer)) {
			Dialogs.showErrorMessage("Error", "Current server is not a BufferedImage-type server.");
			return;
		}
	
		try {
			// Wrap the existing server in your ChromogenImageServer
			var chromServer = new ChromogenImageServer((ImageServer<BufferedImage>) server);
	
			// Build a new ImageData using the same hierarchy
			var transformedData = new ImageData<>(chromServer, imageData.getHierarchy());
	
			// Replace the displayed image
			qupathGUI.getViewer().setImageData(transformedData);
	
			Dialogs.showInfoNotification("Chromogen", "Applied sublinear transform to the image!");
		} catch (IOException e) {
			// If anything triggers an IOException (e.g. reading a tile)
			// handle it gracefully here
			logger.error("Failed to apply Chromogen transform!", e);
			Dialogs.showErrorMessage("Chromogen", "Failed to apply transform:\n" + e.getMessage());
		}
	}


	@Override
	public String getName() {
		return EXTENSION_NAME;
	}

	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}
	
	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}

	@Override
	public GitHubRepo getRepository() {
		return EXTENSION_REPOSITORY;
	}
}
