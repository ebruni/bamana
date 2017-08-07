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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public class Logger {

	static String timestamp;
	
	Path dataErrorsLogPath, metadataErrorsLogPath, copyErrorsLogPath, unreadableLogPath, immediateCheckErrorsLogPath,
			restoreCheckErrorsLogPath, metadataRestoreErrorsLogPath;

	final Object unreadableLogPathFlag = new Object(), copyErrorsLogPathFlag = new Object(),
			dataErrorsLogPathFlag = new Object(), metadataErrorsLogPathFlag = new Object(),
			immediateCheckErrorsLogPathFlag = new Object(), restoreCheckErrorsLogPathFlag = new Object(),
			metadataRestoreErrorLogPathFlag = new Object(); // #162

	// #196
	String UNREADABLE_ELEMENTS_LOG_MESSAGE, DATA_ERROR_LOG_MESSAGE, METADATA_ERROR_LOG_MESSAGE, IMMEDIATE_CHECK_ERROR_LOG_MESSAGE, COPY_ERROR_LOG_MESSAGE;
	
	boolean gotCheckMemToDiskErrors, gotArchiveCheckErrors, gotIoExceptions, gotMetadataRestoreErrors, gotCheckErrors;
	AtomicBoolean gotImmediateCheckErrors, gotUnreadableElements;

	public Logger(String homeDir) {
		timestamp = setTimestamp();
		setLogPaths(homeDir);
		this.gotImmediateCheckErrors = new AtomicBoolean(false);
		this.gotUnreadableElements = new AtomicBoolean(false);
	}
	
	static String setTimestamp() {
		return new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date());
	}

	void setLogPaths(String home) {
		copyErrorsLogPath = Paths.get(home + "/.bamana/logs/" + timestamp + "/errors/copy/copy_errors.log");
		dataErrorsLogPath = Paths.get(home + "/.bamana/logs/" + timestamp + "/errors/data/data_errors.log");
		metadataErrorsLogPath = Paths.get(home + "/.bamana/logs/" + timestamp + "/errors/metadata/metadata_errors.log");
		unreadableLogPath = Paths.get(home + "/.bamana/logs/" + timestamp + "/errors/unreadable/unreadable.log");
		immediateCheckErrorsLogPath = Paths
				.get(home + "/.bamana/logs/" + timestamp + "/errors/immediate_check/immediate_check.log");
		restoreCheckErrorsLogPath = Paths
				.get(home + "/.bamana/logs/" + timestamp + "/errors/restore_check/restore_check.log");
		metadataRestoreErrorsLogPath = Paths
				.get(home + "/.bamana/logs/" + timestamp + "/errors/metadata_restore/metadata_restore.log");
		
		UNREADABLE_ELEMENTS_LOG_MESSAGE = "Errors occurred while trying to read some elements, "
				+ "perhaps due to missing permissions or broken symlinks. See " + unreadableLogPath
				+ " for a complete list of the unreadable elements.";
		DATA_ERROR_LOG_MESSAGE = "Check failed for some elements. See " + dataErrorsLogPath
				+ " for a complete list of the elements that failed the check.";
		METADATA_ERROR_LOG_MESSAGE = "Check failed for some elements. " + "See " + metadataErrorsLogPath
				+ " for a complete list of the elements that failed the check.";
		IMMEDIATE_CHECK_ERROR_LOG_MESSAGE = "Check failed for some elements. " + "See " + immediateCheckErrorsLogPath
				+ " for a complete list of the elements that failed the check.";
		COPY_ERROR_LOG_MESSAGE = "Errors occurred while copying some files to the backup's archive. " + "See "
				+ copyErrorsLogPath + " for a complete list of the uncopied files.";
	}
	
	public Path logFatalException(Exception ex, String home) throws IOException {
		String timestamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new java.util.Date()); // #189
		Path logPath = Paths.get(home + "/.bamana/" + timestamp + ".log");
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		IOutilities.createFileIfNotExists(logPath);
		IOutilities.appendLineToFile(sw.toString(), logPath);
		return logPath;
	}

	public void manageLoggerError(IOException loggerException, String originalError) { // #190
		System.out.println();
		System.out.println("LOGGER ERROR:");
		loggerException.printStackTrace();
		System.out.println();
		System.out.println("ERROR THAT BAMANA WAS TRYING TO LOG TO FILE:");
		System.out.println(originalError);
	}

	public void log(String line, Path path) throws IOException {
		IOutilities.createDirectories((path).getParent());
		IOutilities.createFileIfNotExists(path);
		IOutilities.appendLineToFile(line, path);
	}

	void logUnreadable(String line) { // #191
		synchronized (unreadableLogPathFlag) {
			try {
				log(line, unreadableLogPath);
			} catch (IOException e) {
				String originalError = "Can't read from " + line;
				manageLoggerError(e, originalError);
				return;
			}
			boolean firstTime = gotUnreadableElements.compareAndSet(false, true);
			if (firstTime)
				System.out.println("\n" + UNREADABLE_ELEMENTS_LOG_MESSAGE);
		}
	}

	void logDataError(String line) {
		try {
			log(line, dataErrorsLogPath); // #34?
		} catch (IOException loggerException) {
			String originalError = "Data check failed for " + line;
			manageLoggerError(loggerException, originalError);
			return;
		}
		if (!gotArchiveCheckErrors) {
			gotArchiveCheckErrors = true;
			System.out.println("\n" + DATA_ERROR_LOG_MESSAGE);
		}
	}

	void logMetadataError(String lineToLog) {
		try {
			log(lineToLog, metadataErrorsLogPath);
		} catch (IOException e) {
			String originalError = "Metadata check failed:\n" + lineToLog;
			manageLoggerError(e, originalError);
			return;
		}
		if (!gotCheckMemToDiskErrors) {
			gotCheckMemToDiskErrors = true;
			System.out.println("\n" + METADATA_ERROR_LOG_MESSAGE);
		}
	}

	void logImmediateCheckError(String line) {
		synchronized (immediateCheckErrorsLogPathFlag) {
			try {
				log(line, immediateCheckErrorsLogPath);
			} catch (IOException e) {
				String originalError = "Data check failed for " + line;
				manageLoggerError(e, originalError);
				return;
			} // #176
			boolean firstTime = gotImmediateCheckErrors.compareAndSet(false, true);
			if (firstTime)
				System.out.println("\n" + IMMEDIATE_CHECK_ERROR_LOG_MESSAGE);
		}
	}

	void logCopyError(String line) {
		try {
			log(line, copyErrorsLogPath);
		} catch (IOException e) {
			String originalError = "Copy failed for " + line;
			manageLoggerError(e, originalError);
			return;
		}
		if (!gotIoExceptions) {
			gotIoExceptions = true;
			System.out.println("\n" + COPY_ERROR_LOG_MESSAGE);
		}
	}

	void logMetadataRestoreError(String restoredFilePath) { // #170
		try {
			log(restoredFilePath, metadataRestoreErrorsLogPath);
		} catch (IOException e) {
			String originalError = "Can't apply metadata to " + restoredFilePath;
			manageLoggerError(e, originalError);
		}
		if (!gotMetadataRestoreErrors) {
			gotMetadataRestoreErrors = true;
			System.out.println("\nCouldn't apply metadata to some elements. " + "See "
					+ metadataRestoreErrorsLogPath + " for a complete list.");
		}
	}

	void logCheckError(String line) {
		try {
			log(line, restoreCheckErrorsLogPath);
		} catch (IOException e) {
			String originalError = "Check failed for " + line;
			manageLoggerError(e, originalError);
		}
		if (!gotCheckErrors) {
			gotCheckErrors = true;
			System.out.println("\nCheck failed for some elements. See " + restoreCheckErrorsLogPath
					+ " for a complete list of the elements that failed the check.");
		}
	}
}
