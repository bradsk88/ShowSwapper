package ca.bradj.showswap.mv;

import java.io.File;
import java.nio.file.Path;

import com.google.common.base.Optional;

public class Builder {

	private Optional<Path> destPath = Optional.absent();
	private Optional<String> showName = Optional.absent();
	private Optional<File> sourceFile;

	Builder() {
	}

	public Builder destinationFolder(File destPath) {
		this.destPath = Optional.of(destPath.toPath());
		return this;
	}

	public Builder showName(String showName) {
		this.showName = Optional.of(showName);
		return this;
	}

	public Builder showSourceFile(File upperName) {
		this.sourceFile = Optional.of(upperName);
		return this;
	}

	public MoveInfo build() {
		return new MoveInfo(destPath.get(), showName.get(), sourceFile.get());
	}
}
