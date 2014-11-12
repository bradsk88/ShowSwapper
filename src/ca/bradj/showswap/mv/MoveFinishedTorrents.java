package ca.bradj.showswap.mv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bradj.common.base.Failable;
import ca.bradj.common.base.Preconditions2;
import ca.bradj.gsmatch.Match;
import ca.bradj.gsmatch.Matching;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

public class MoveFinishedTorrents implements Runnable {

    public final Logger LOGGER = LoggerFactory.getLogger(MoveFinishedTorrents.class);
    private final StrongMatchProvider prefs;
    private final StrongMatchProvider films;
    private final AlreadyTransferred alreadyTransferred;
    private Path destinationTVFolder;
    private Path finishedfolder;
    private final String unrarCommand;
    private AllowTransferRandomization allowRandomSkips;

    public MoveFinishedTorrents(StrongMatchProvider shows, StrongMatchProvider films, AlreadyTransferred already, Path destinationTVFolder,
            Path finishedTorrentsDir, String unrarCmd, AllowTransferRandomization allowRandomSkips) {
        this.destinationTVFolder = Preconditions.checkNotNull(destinationTVFolder);
        this.prefs = Preconditions.checkNotNull(shows);
        this.films = Preconditions.checkNotNull(films);
        this.alreadyTransferred = Preconditions.checkNotNull(already);
        this.finishedfolder = Preconditions.checkNotNull(finishedTorrentsDir);
        this.unrarCommand = Preconditions2.checkNotEmpty(unrarCmd);
        this.allowRandomSkips = Preconditions.checkNotNull(allowRandomSkips);
    }

    @Override
    public void run() {

        try {

            if (Files.notExists(finishedfolder)) {
                LOGGER.error(finishedfolder.toString() + " didn't exist.  Cannot manage files.");
                return;
            }

            int year = DateTime.now().year().get();
            File file = new File(destinationTVFolder.toFile(), Integer.toString(year));
            File[] listFiles = finishedfolder.toFile().listFiles();
            for (File i : listFiles) {
                processFile(file, i);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

    }

    private void processFile(File file, File i) {
        LOGGER.debug("Assessing " + i.getName() + " for move");
        try {
            String filename = i.getName();
            if (Matching.hasStrongMatch(alreadyTransferred.getList(), filename)) {
                LOGGER.debug("An exact match has already been moved for " + filename + ", skipping it.");
                return;
            }
            Failable<Match> showName = prefs.getStrongestMatch(i.getName());
            if (showName.isFailure()) {
                if (films.getStrongestMatch(i.getName()).isSuccess()) {
                    LOGGER.error("{} was a film, but management of film files is not currently supported", i.getName());
                    return;
                }
                LOGGER.debug("Didn't recognize as a show or film: {}", i.getName());
                return;
            }

            String name = showName.get().getName();
            LOGGER.debug("Moving " + name + " now");

            try {
                moveFile(file, i, name);
            } catch (Exception e) {
                LOGGER.error("An exception occurred during the file transfer for " + name + "\n" + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Exception while processing: " + i.getName());
        }
    }

    private void moveFile(File destPath, File i, String showName) {

        MoveInfo moveInfo = MoveInfo.create().destinationFolder(destPath).showName(showName).showSourceFile(i).build();
        LOGGER.debug("Move process initiated for: " + moveInfo);

        if (i.isDirectory()) {
            if (directoryContainsIncompleteSetOfRARFiles(i)) {
                LOGGER.error("Directory contains incomple set of RAR files: " + i);
                return;
            }
            for (File f : i.listFiles()) {
                if (isRAR(f)) {
                    unRARAndMove(moveInfo, f, false);
                    return;
                }
            }
            return;
        }
        Failable<String> moveType = isMoveType(i);
        if (moveType.isSuccess()) {
            File destination = moveInfo.getDestinationFile();
            if (Files.exists(destination.toPath())) {
                return;
            }
            doMoveFile(i, moveInfo, false);
        }
    }

    private Optional<Failable<File>> unRARAndMove(MoveInfo moveInfo, File file, boolean isNested) {

        // flip a coin. If heads, unrar this file and check if it
        // needs to be sent to the server. This random aspect is
        // just here to reduce disk usage. The file WILL eventually
        // be transferred.
        if (!isNested && allowRandomSkips == AllowTransferRandomization.YES && new Random().nextBoolean()) {
            return Optional.absent();
        }

        Failable<File> unrarred = unrar(file, moveInfo);
        if (isNested) {
            return Optional.of(unrarred);
        }
        if (unrarred.isSuccess()) {
            doMoveFile(unrarred.get(), moveInfo.withSource(unrarred.get()), true);
        } else {
            LOGGER.debug(unrarred.getReason());
        }
        return Optional.absent();
    }

    private boolean isRAR(File f) {
        if (f.getName().toLowerCase().endsWith(".rar")) {
            return true;
        }
        return false;
    }

    Failable<File> unrar(File f, MoveInfo moveInfo) {

        try {
            String command = unrarCommand + " " + f.getAbsolutePath();
            if (command.contains("%RARFILE%")) {
                command = unrarCommand.replace("%RARFILE%", f.getAbsolutePath());
            }
            if (command.contains("%RARFOLDER%")) {
                command = command.replace("%RARFOLDER%", f.getParent());
            }
            File parent = f.getParentFile();
            Process exec = Runtime.getRuntime().exec(command);
            LOGGER.debug("Command is : " + command);
            LOGGER.debug("Waiting for " + f.getName() + " unrar to complete");
            try (@SuppressWarnings("resource")
            BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()))) {
                String line = reader.readLine();
                while (line != null) {
                    if (line.trim().startsWith("...")) {
                        line = reader.readLine();
                        continue;
                    }
                    if (line.trim().length() > 0) {
                        LOGGER.debug(line);
                    }
                    line = reader.readLine();
                }
                exec.waitFor();
                LOGGER.debug("Unrar of " + f.getName() + " complete");

                if (directoryContainsIncompleteSetOfRARFiles(parent)) {
                    return Failable.fail("Directory contains incomplete set of RAR files: " + parent);
                }

                ArrayList<File> listFiles = Lists.newArrayList(parent.listFiles());
                for (File i : Iterables.filter(listFiles, isNot(f))) {
                    if (isMoveType(i).isSuccess()) {
                        return Failable.ofSuccess(i);
                    }
                    if (isRAR(i)) {
                        // nested RARs
                        Optional<Failable<File>> moved = unRARAndMove(moveInfo, i, true);
                        if (moved.isPresent()) {
                            return moved.get();
                        }
                    }
                }
                return Failable.fail("Unrarred " + f.getName() + ", but valid file not present");
            }
        } catch (Exception e) {
            LOGGER.info("NOTIF: " + e.getMessage());
            e.printStackTrace();
            return Failable.fail(e.getMessage());
        }
    }

    private Predicate<File> isNot(final File f) {
        return new Predicate<File>() {

            @Override
            public boolean apply(File arg0) {
                return !f.equals(arg0);
            }
        };
    }

    private boolean directoryContainsIncompleteSetOfRARFiles(File parent) {

        int expectedNumber = 0;
        boolean gapped = false;
        File[] listFiles = parent.listFiles();
        Collection<File> sortedListFiles = Ordering.<File> natural().sortedCopy(Lists.newArrayList(listFiles));
        for (File i : sortedListFiles) {
            if (i.getPath().endsWith("\\.rar")) {
                continue;
            }
            Optional<Integer> n = getRARNumber(i);
            if (n.isPresent()) {
                if (gapped) {
                    return true;
                }
                if (expectedNumber == n.get()) {
                    expectedNumber++;
                    continue;
                }
                gapped = true;
                expectedNumber++;
            }
        }
        return false;
    }

    private Optional<Integer> getRARNumber(File i) {
        String path = i.getPath();
        String extension = path.substring(path.length() - 3, path.length());
        Matcher matcher = Pattern.compile("r([0-9][0-9])").matcher(extension);
        if (matcher.matches()) {
            try {
                int parseInt = Integer.parseInt(matcher.group(1));
                return Optional.of(parseInt);
            } catch (NumberFormatException e) {
                return Optional.absent();
            }
        }
        return Optional.absent();
    }

    private Failable<String> isMoveType(File i) {
        if (i.getName().endsWith(".mkv")) {
            return Failable.ofSuccess("mkv");
        }
        if (i.getName().endsWith(".avi")) {
            return Failable.ofSuccess("avi");
        }
        if (i.getName().endsWith(".mp4")) {
            return Failable.ofSuccess("mp4");
        }
        return Failable.fail("Unrecognized filetype on file: " + i.getName());
    }

    private void doMoveFile(File src, MoveInfo moveInfo, boolean deleteAfterMove) {
        File dest = moveInfo.getDestinationFile();
        if (Files.exists(dest.toPath()) && src.length() == dest.length()) {
            if (deleteAfterMove) {
                delete(src);
            }
            alreadyTransferred.addAndWriteToDisk(moveInfo.getPrettyName());
            return;
        }
        if (Files.notExists(dest.getParentFile().toPath())) {
            dest.getParentFile().mkdirs();
        }
        try (FileInputStream fis = new FileInputStream(src); FileOutputStream fos = new FileOutputStream(dest)) {
            LOGGER.info("NOTIF: Transferring file " + src.getName() + " to " + dest.getAbsolutePath());
            fos.getChannel().transferFrom(fis.getChannel(), 0, fis.getChannel().size());
            LOGGER.debug("Transfer ended for " + src.getName());
        } catch (Exception e) {
            LOGGER.info("NOTIF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (deleteAfterMove) {
                delete(src);
            }
            alreadyTransferred.addAndWriteToDisk(moveInfo.getPrettyName());
        }

    }

    private void delete(File src) {
        boolean delete = src.delete();
        if (!delete) {
            LOGGER.error("Could not delete " + src.getPath());
        }
    }
}
