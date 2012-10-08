/*
 * Updater for Bukkit.
 *
 * This class provides the means to safetly and easily update a plugin, or check to see if it is updated using dev.bukkit.org
 */

package net.h31ix.updater;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
/**
 * Check dev.bukkit.org to find updates for a given plugin, and download the updates if needed.
 * <p>
 * <b>VERY, VERY IMPORTANT</b>: Because there are no standards for adding auto-update toggles in your plugin's config, this system provides NO CHECK WITH YOUR CONFIG to make sure the user has allowed auto-updating.
 * <br>
 * It is a <b>BUKKIT POLICY</b> that you include a boolean value in your config that prevents the auto-updater from running <b>AT ALL</b>.
 * <br>
 * If you fail to include this option in your config, your plugin will be <b>REJECTED</b> when you attempt to submit it to dev.bukkit.org.
 * <p>
 * An example of a good configuration option would be something similar to 'auto-update: true' - if this value is set to false you may NOT run the auto-updater.
 * <br>
 * If you are unsure about these rules, please read the plugin submission guidelines: http://goo.gl/8iU5l
 *
 * @author H31IX
 */

public class Updater
{
    private String versionTitle;
    private long totalSize; // Holds the total size of the file
    //private double downloadedSize; TODO: Holds the number of bytes downloaded
    private URL url; // Connecting to RSS
    private static final String DBOUrl = "http://dev.bukkit.org/server-mods/"; // Slugs will be appended to this to get to the project's RSS feed
    private Updater.UpdateResult result = Updater.UpdateResult.SUCCESS; // Used for determining the outcome of the update process

    // Strings for reading RSS
    private static final String TITLE = "title";
    private static final String LINK = "link";
    private static final String ITEM = "item";

    /**
    * Gives the dev the result of the update process. Can be obtained by called getResult().
    */
    public enum UpdateResult
    {
        /**
        * The updater found an update, and has readied it to be loaded the next time the server restarts/reloads.
        */
        SUCCESS(1),
        /**
        * The updater did not find an update, and nothing was downloaded.
        */
        NO_UPDATE(2),
        /**
        * The updater found an update, but was unable to download it.
        */
        FAIL_DOWNLOAD(3),
        /**
        * For some reason, the updater was unable to contact dev.bukkit.org to download the file.
        */
        FAIL_DBO(4),
        /**
        * When running the version check, the file on DBO did not contain the a version in the format 'vVersion' such as 'v1.0'.
        */
        FAIL_NOVERSION(5),
        /**
        * The slug provided by the plugin running the updater was invalid and doesn't exist on DBO.
        */
        FAIL_BADSLUG(6),
        /**
        * The updater found an update, but because of the UpdateType being set to NO_DOWNLOAD, it wasn't downloaded.
        */
        UPDATE_AVAILABLE(7);

        private static final Map<Integer, Updater.UpdateResult> valueList = new HashMap<Integer, Updater.UpdateResult>();
        private final int value;

        private UpdateResult(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return this.value;
        }

        public static Updater.UpdateResult getResult(int value)
        {
            return valueList.get(value);
        }

        static
        {
            for(Updater.UpdateResult result : Updater.UpdateResult.values())
            {
                valueList.put(result.value, result);
            }
        }
    }

    /**
    * Allows the dev to specify the type of update that will be run.
    */
    public enum UpdateType
    {
        /**
        * Run a version check, and then if the file is out of date, download the newest version.
        */
        DEFAULT(1),
        /**
        * Don't run a version check, just find the latest update and download it.
        */
        NO_VERSION_CHECK(2),
        /**
        * Get information about the version and the download size, but don't actually download anything.
        */
        NO_DOWNLOAD(3);

        private static final Map<Integer, Updater.UpdateType> valueList = new HashMap<Integer, Updater.UpdateType>();
        private final int value;

        private UpdateType(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return this.value;
        }

        public static Updater.UpdateType getResult(int value)
        {
            return valueList.get(value);
        }

        static
        {
            for(Updater.UpdateType result : Updater.UpdateType.values())
            {
                valueList.put(result.value, result);
            }
        }
    }

    /**
     * Initialize the updater
     *
     * @param plugin
     *            The plugin that is checking for an update.
     * @param slug
     *            The dev.bukkit.org slug of the project (http://dev.bukkit.org/server-mods/SLUG_IS_HERE)
     * @param file
     *            The file that the plugin is running from, get this by doing this.getFile() from within your main class.
     * @param type
     *            Specify the type of update this will be. See {@link UpdateType}
     * @param announce
     *            True if the program should announce the progress of new updates in console
     */
    public Updater(String slug)
    {
        try
        {
            // Obtain the results of the project's file feed
            url = new URL(DBOUrl + slug + "/files.rss");
        }
        catch (MalformedURLException ex)
        {
            // The slug doesn't exist
            result = Updater.UpdateResult.FAIL_BADSLUG; // Bad slug! Bad!
        }
        if(url != null)
        {
            // Obtain the results of the project's file feed
            readFeed();
        }
    }

    /**
     * Get the result of the update process.
     */
    public Updater.UpdateResult getResult()
    {
        return result;
    }

    /**
     * Get the total bytes of the file (can only be used after running a version check or a normal run).
     */
    public long getFileSize()
    {
        return totalSize;
    }

    /**
     * Get the version string latest file avaliable online.
     */
    public String getLatestVersionString()
    {
        return versionTitle;
    }

    /**
     * Check if the name of a jar is one of the plugins currently installed, used for extracting the correct files out of a zip.
     */
    public boolean pluginFile(String name)
    {
        for(File file : new File("plugins").listFiles())
        {
            if(file.getName().equals(name))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Part of RSS Reader by Vogella, modified by H31IX for use with Bukkit
     */
    private void readFeed()
    {
        try
        {
            // Set header values intial to the empty string
            String title = "";
            String link = "";
            // First create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // Setup a new eventReader
            InputStream in = read();
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
            // Read the XML document
            while (eventReader.hasNext())
            {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement())
                {
                    if (event.asStartElement().getName().getLocalPart().equals(TITLE))
                    {
                        event = eventReader.nextEvent();
                        title = event.asCharacters().getData();
                        continue;
                    }
                    if (event.asStartElement().getName().getLocalPart().equals(LINK))
                    {
                        event = eventReader.nextEvent();
                        link = event.asCharacters().getData();
                        continue;
                    }
                }
                else if (event.isEndElement())
                {
                    if (event.asEndElement().getName().getLocalPart().equals(ITEM))
                    {
                        // Store the title and link of the first entry we get - the first file on the list is all we need
                        versionTitle = title;
                        // All done, we don't need to know about older files.
                        break;
                    }
                }
            }
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Open the RSS feed
     */
    private InputStream read()
    {
        try
        {
            return url.openStream();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}