package ca.bradj.showswap.mv;

import java.io.File;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import ca.bradj.common.base.Failable;
import ca.bradj.common.base.Preconditions2;
import ca.bradj.gsmatch.EpisodeID;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class MoveInfo {

    private final Path destPath;
    private final String showName;
    private Optional<Path> unrarred;
    private final String upperName;

    public MoveInfo(Path path, String showName, File sourceFileName) {
        this.destPath = Preconditions.checkNotNull(path);
        this.showName = Preconditions2.checkNotEmpty(showName);
        String cleanedShowName = cleanFilename(showName, sourceFileName);
        this.upperName = cleanedShowName;
    }

    private String cleanFilename(String showName, File sourceFileName) {
        String cleanedShowName = sourceFileName.getName();
        Failable<EpisodeID> parse = EpisodeID.parse(sourceFileName.getName());
        if (parse.isSuccess()) {
            EpisodeID epId = parse.get();
            StringBuilder sb = new StringBuilder();
            sb.append("S");
            if (epId.getSeason() < 10) {
                sb.append("0");
            }
            sb.append(epId.getSeason());
            sb.append("E");
            if (epId.getEpisode() < 10) {
                sb.append("0");
            }
            sb.append(epId.getEpisode());
            cleanedShowName = showName + " " + sb.toString() + "." + FilenameUtils.getExtension(sourceFileName.getName());
        }
        return cleanedShowName;
    }

    public static Builder create() {
        return new Builder();
    }

    public File getDestinationFile() {
        return new File(destPath + File.separator + showName + File.separator + upperName);
    }

    public String getPrettyName() {
        return upperName;
    }

    @Override
    public String toString() {
        return "MoveInfo [destPath=" + destPath + ", showName=" + showName + ", unrarred=" + unrarred + ", upperName=" + upperName + "]";
    }

    public MoveInfo withSource(File file) {
        return new MoveInfo(destPath, showName, file);
    }

}
