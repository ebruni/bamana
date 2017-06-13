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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;

public class Bamana {

	static Logger logger;

	public static void main(String[] args) {

		String home;
		// #85
		if (args[0].equals("init")) {
			home = args[4];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new BackupInitializer(Arrays.copyOfRange(args, 1, args.length)).init();
			} catch (IOException e) {
				catched(e, home);
			}
		} else if (args[0].equals("snap")) {
			home = args[3];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new Snapper(logger, Arrays.copyOfRange(args, 1, args.length)).snap();
			} catch (Exception e) {
				catched(e, home);
			}
		} else if (args[0].equals("restore")) {
			home = args[4];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new SnapshotRestorer(logger, Arrays.copyOfRange(args, 1, args.length)).restore();
			} catch (Exception e) {
				catched(e, home);
			}
			System.out.println();
		} else if (args[0].equals("listb")) {
			home = args[1];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new BackupLister(Arrays.copyOfRange(args, 1, args.length)).list();
			} catch (IOException e) {
				catched(e, home);
			}
		} else if (args[0].equals("lists")) {
			home = args[2];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new SnapshotLister(Arrays.copyOfRange(args, 1, args.length)).list();
			} catch (IOException e) {
				catched(e, home);
			}
		} else if (args[0].equals("cap")) {
			home = args[3];
			logger = new Logger(home);
			logger.setLogPaths(home);
			try {
				new ArchivePathChanger(Arrays.copyOfRange(args, 1, args.length)).cap();
			} catch (IOException e) {
				catched(e, home);
			}
		}
	}

	private static void catched(Exception e, String home) {
		System.out.println();
		Path logPath = null;
		try {
			logPath = logger.logFatalException(e, home);
		} catch (IOException loggerException) { // #160
			System.out.println("FATAL ERROR");
			logger.manageLoggerError(loggerException, e.getMessage());
			System.exit(1);
		}
		System.out.println();
		System.out.println(e.getMessage());
		System.out.println("FATAL ERROR: see the log at " + logPath.toString());
		System.exit(1);
	}

}