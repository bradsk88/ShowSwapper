package ca.bradj.showswap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bradj.showswap.daemon.FileToXBMCDaemon;
import ca.bradj.showswap.mv.AlreadyTransferred;

public class Main {

    public static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {

        File configFile = new File("config.properties");

        String tvdest = null;
        String tvsrc = null;
        String tvprefs = null;
        String unrarCmd = null;

        try {
            FileReader reader = new FileReader(configFile);
            Properties props = new Properties();
            props.load(reader);

            tvdest = props.getProperty("tvdest");
            tvsrc = props.getProperty("tvsrc");
            tvprefs = props.getProperty("prefs");
            unrarCmd = props.getProperty("unrarcmd");
            reader.close();
        } catch (FileNotFoundException ex) {
            // file does not exist
        } catch (IOException ex) {
            // I/O error
        }

        FileWriter writer = new FileWriter(configFile);
        Properties props = new Properties();
        boolean destOk = checkProp(props, configFile, "No TV Destination folder set.", "tvdest", tvdest);
        boolean srcOk = checkProp(props, configFile, "No TV Source folder set.", "tvsrc", tvsrc);
        boolean prefOk = checkProp(props, configFile, "No Known Show-Names folder set.", "prefs", tvprefs);
        boolean unrarOK = checkProp(props, configFile, "Unrar command not set.", "unrarcmd", unrarCmd);
        props.store(writer, "Settings");
        writer.close();

        if (destOk && srcOk && prefOk && unrarOK) {
            Path tvPrefsFile = Paths.get(tvprefs);
            Path destTVFolder = Paths.get(tvdest);
            AlreadyTransferred already = AlreadyTransferred.load(destTVFolder);
            Path finishedDir = Paths.get(tvsrc);
            new FileToXBMCDaemon(tvPrefsFile, already, destTVFolder, finishedDir, unrarCmd).start();
        } else {
            LOGGER.error("Configuration values were missing.  See error messages above");
        }

    }

    private static boolean isUnset(String tvsrc) {
        return tvsrc == null || tvsrc.isEmpty();
    }

    private static boolean checkProp(Properties props, File configFile, String notSetMsg, String propName, String wantedProp) throws IOException {
        if (isUnset(wantedProp)) {
            System.out.println(notSetMsg);
            props.setProperty(propName, "");
            System.out.println("Please set the value of \"" + propName + "\" in " + configFile.getAbsolutePath());
            return false;
        }
        props.setProperty(propName, wantedProp);
        return true;
    }

}
