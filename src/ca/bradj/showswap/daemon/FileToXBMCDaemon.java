package ca.bradj.showswap.daemon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.bradj.common.base.Preconditions2;
import ca.bradj.showswap.mv.AllowTransferRandomization;
import ca.bradj.showswap.mv.AlreadyTransferred;
import ca.bradj.showswap.mv.MoveFinishedTorrents;
import ca.bradj.showswap.mv.StrongMatchProvider;
import ca.bradj.showswap.mv.StrongMatchProviders;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;

public class FileToXBMCDaemon {

    private final SimpleStringProperty countDownProperty = new SimpleStringProperty();
    private final ScheduledExecutorService moveEx = Executors.newScheduledThreadPool(1);
    private final Logger LOGGER = LoggerFactory.getLogger(FileToXBMCDaemon.class);
    private final AlreadyTransferred already;
    protected final Path destinationTVFolder;
    protected final Path finishedTorrentsDir;
    private final String unrarCommand;
    private final Path tvPrefsFile;

    public FileToXBMCDaemon(Path tvPrefsFile, AlreadyTransferred already, Path destTVFolder, Path finishedDir, String unrarCmd) {
        this.tvPrefsFile = Preconditions.checkNotNull(tvPrefsFile);
        this.already = already;
        this.destinationTVFolder = Preconditions.checkNotNull(destTVFolder);
        this.finishedTorrentsDir = Preconditions.checkNotNull(finishedDir);
        this.unrarCommand = Preconditions2.checkNotEmpty(unrarCmd);
    }

    public ScheduledExecutorService start() {
        LOGGER.debug("Starting file transfer service");
        moveEx.scheduleAtFixedRate(moveTorrentsAndStartCountDown(), 10, 20 * 60, TimeUnit.SECONDS);
        return moveEx;
    }

    private Runnable moveTorrentsAndStartCountDown() {
        return new Runnable() {

            @Override
            public void run() {

                LOGGER.info("File Transfer daemon is about to do work.");

//                self = countEx.scheduleAtFixedRate(new Runnable() {
//
//                    final AtomicInteger seconds = new AtomicInteger(20 * 60);
//
//                    @Override
//                    public void run() {
//                        FXThreading.invokeLater(new Runnable() {
//                            @Override
//                            public void run() {
//                                countDownProperty.set(countdownWillBeginIn(seconds.get()));
//                            }
//                        });
//                        if (seconds.decrementAndGet() <= 0) {
//                            self.cancel(true);
//                        }
//                    }
//                }, 0, 1, TimeUnit.SECONDS);

                List<String> lines;
                try {
                    lines = Files.readLines(tvPrefsFile.toFile(), Charsets.UTF_8);
                } catch (IOException e1) {
                    LOGGER.error(e1.getMessage());
                    e1.printStackTrace();
                    return;
                }

                try {
                    StrongMatchProvider myShows = StrongMatchProviders.fromList(lines);
                    StrongMatchProvider myFilms = StrongMatchProviders.empty();
                    new MoveFinishedTorrents(myShows, myFilms, already, destinationTVFolder, finishedTorrentsDir, unrarCommand, allow())
                            .run();
                } catch (Exception e) {
                    e.printStackTrace();
                    LOGGER.error(e.getMessage());
                }

            }
        };
    }

    protected AllowTransferRandomization allow() {
        // if (Config.ALLOW_TRANSFER_RANDOMIZATION) {
        // return AllowTransferRandomization.YES;
        // }
        return AllowTransferRandomization.NO;
    }

    protected String countdownWillBeginIn(int i) {
        return "File transfers will begin in " + i + " seconds.";
    }

    public ObservableValue<? extends String> countDownProperty() {
        return countDownProperty;
    }

}
