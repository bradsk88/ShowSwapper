package ca.bradj.showswap.mv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

import ca.bradj.common.base.Record;

public class AlreadyTransferred extends Record<AlreadyTransferred> {

	private static final String THISDIR = "transfered";

	public AlreadyTransferred(Path root, Collection<String> livePrefs2) {
		super(root, THISDIR, livePrefs2);
	}

	public static AlreadyTransferred load(Path root) throws IOException {
		File f = new File(root + File.separator + THISDIR);
		if (f.exists()) {
			return new AlreadyTransferred(root, Record.parse(f));
		}
		return AlreadyTransferred.empty(root);
	}

	@SuppressWarnings("unchecked")
	private static AlreadyTransferred empty(Path root) {
		return new AlreadyTransferred(root, Collections.EMPTY_LIST);
	}

	@Override
	public String toString() {
		return "AlreadyTransferred [" + super.toString() + "]";
	}

}
