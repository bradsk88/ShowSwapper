package ca.bradj.showswap.mv;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;

import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bradj.common.base.Failable;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class MoveTest {

    public static Logger LOGGER = LoggerFactory.getLogger(MoveTest.class);

    @Test
    public void testNamedFileGetsMovedToCorrectFolder() throws IOException {
        File baseDir = Files.createTempDir();
        File source = new File(baseDir, "Source");
        source.mkdirs();
        File file = new File(source, "My.Show.S01E01.SomeDUDES.h264.mkv");
        FileWriter fw = new FileWriter(file);
        fw.write("arbitraryData");
        fw.close();
        file.createNewFile();
        Iterable<String> showNames = Lists.newArrayList("My Show", "Another Show");

        File destination = new File(baseDir, "destination");
        destination.mkdirs();

        Iterable<String> filmNames = Collections.emptyList();
        AlreadyTransferred already = Mockito.mock(AlreadyTransferred.class);
        MoveFinishedTorrents mover = new MoveFinishedTorrents(StrongMatchProviders.fromList(showNames),
                StrongMatchProviders.fromList(filmNames), already, destination.toPath(), source.toPath(), "omitted",
                 AllowTransferRandomization.NO);
        mover.run();

        File expectedFile = new File(destination, Integer.toString(DateTime.now().getYear()) + File.separator + "My Show" + File.separator
                + "My Show S01E01.mkv");
        assertTrue(expectedFile.exists());

    }

    @Test
    public void testRARdFileGetsMovedToCorrectFolder() throws IOException {
        File baseDir = Files.createTempDir();
        File source = new File(baseDir, "Source" );
        final File rarDir = new File(source, "My.Show.S01E01.SomeDUDES.h264" );
        rarDir.mkdirs();
        new File(rarDir, "My.Show.S01E01.SomeDUDES.h264.rar").createNewFile();
        new File(rarDir, "My.Show.S01E01.SomeDUDES.h264.r00").createNewFile();
        new File(rarDir, "My.Show.S01E01.SomeDUDES.h264.r01").createNewFile();
        new File(rarDir, "My.Show.S01E01.SomeDUDES.h264.r02").createNewFile();

        Iterable<String> showNames = Lists.newArrayList("My Show", "Another Show");

        File destination = new File(baseDir, "destination");
        destination.mkdirs();

        Iterable<String> filmNames = Collections.emptyList();
        AlreadyTransferred already = Mockito.mock(AlreadyTransferred.class);
        MoveFinishedTorrents mover = new MoveFinishedTorrents(StrongMatchProviders.fromList(showNames),
                StrongMatchProviders.fromList(filmNames), already, destination.toPath(), source.toPath(), "omitted",
                 AllowTransferRandomization.NO) {
            @Override
            ca.bradj.common.base.Failable<File> unrar(File f, MoveInfo moveInfo) {
                File out = new File(rarDir, "My.Show.S01E01.SomeDUDES.h264.mp4");
                try (FileWriter fw = new FileWriter(out)) {
                    fw.write("arbitraryBits");
                } catch (IOException e) {
                    throw new AssertionError(e.getClass() + ": " + e.getMessage());
                }
                return Failable.ofSuccess(out);
            }
        };
        mover.run();

        File expectedFile = new File(destination, Integer.toString(DateTime.now().getYear()) + File.separator + "My Show" + File.separator
                + "My Show S01E01.mp4");
        assertTrue(expectedFile.exists());

    }

}
