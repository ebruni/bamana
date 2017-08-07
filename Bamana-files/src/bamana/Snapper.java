/*
 * Bamana - a free incremental backup software for GNU/Linux
 * Copyright (C) 2017 Emanuele Bruni
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bamana;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Snapper {

	String name;
	Path sourcePath, homePath, dotBamanaPath, backupsPath;
	String[] args;
	Logger logger;

	public Snapper(Logger logger, String[] args) {
		this.args = args;
		this.logger = logger;
		sourcePath = Paths.get(args[0]); // #94
		name = args[1];
		homePath = Paths.get(args[2]);
		dotBamanaPath = Paths.get(homePath + "/.bamana");
		backupsPath = Paths.get(dotBamanaPath + "/backups.txt");
		// #143, #96
	}

	public void snap() throws Exception { // #97
		Path deserializedPath;
		String backupName = null, archive = null;
		List<String> deserializedBackups = StaticTools.deserializeBackups(backupsPath);
		boolean backupInitialized = false;
		Iterator<String> it = deserializedBackups.iterator();
		while (it.hasNext()) {
			List<String> deserializedBackup = StaticTools.deserializeBackup(it.next());
			deserializedPath = Paths.get(deserializedBackup.get(1));
			if (deserializedPath.equals(sourcePath)) { // #99
				backupName = deserializedBackup.get(0);
				archive = deserializedBackup.get(2);
				backupInitialized = true;
				break;
			}
		}
		if (!backupInitialized) {
			System.out.println("Please initialize your backup with 'bmn init -i' before running a snapshot.");
			return;
		}
		System.out.println("Initializing the snapshot...");
		String[] quotedExcludedPaths = Arrays.copyOfRange(args, 4, args.length);
		String[] excludedPaths = new String[quotedExcludedPaths.length];
		for (int i = 0; i < quotedExcludedPaths.length; i++) {
			excludedPaths[i] = quotedExcludedPaths[i].substring(1,
					quotedExcludedPaths[i].length() - 1);
		}
		MultiThreadSnapshotCreator cs = new MultiThreadSnapshotCreator(archive, sourcePath.toString(), name, excludedPaths, backupName, logger);
		cs.snap(sourcePath);
	} // #101

}
