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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class IOutilities {

	static String readLine(BufferedReader br, Path path) throws IOException {
		try {
			return br.readLine();
		} catch (IOException e) {
			throw new IOException("Can't read from file: " + path, e);
		}
	}

	static void deleteFileIfExists(Path path) throws IOException {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new IOException("Can't delete " + path);
		}
	}
	
	static boolean createFileIfNotExists(Path path) throws IOException { // #87, #186
		if (Files.notExists(path) || !path.toFile().exists()) {
			Files.createFile(path); // #88
			return true;
		}
		return false;
	}

	static void writeStringAndNewLineToBufferedWriter(String line, BufferedWriter writer, Path path) throws IOException {
		try {
			writer.write(line);
			writer.newLine();
		} catch (IOException ex) {
			throw new IOException("Can't write to " + path, ex);
		}
	}
	
	static void flushAndCloseWriter(BufferedWriter writer, Path path) throws IOException {
		try {
			writer.flush();
			writer.close();
		} catch (IOException ex) {
			throw new IOException("Error while flushing & closing writer to " + path.toString(), ex);
		}
	}
	
	static void moveFile(Path tempPath, Path defPath) throws IOException {
		try {
			Files.move(tempPath, defPath, StandardCopyOption.REPLACE_EXISTING); // #187
		} catch (FileAlreadyExistsException faee) {
			throw new FileAlreadyExistsException(defPath.toString(), tempPath.toString(),
					"File already exists: " + defPath); // #17
		} catch (IOException ex) { // #18
			throw new IOException("Can't move " + tempPath + " to " + defPath + ".", ex);
		}
	}
	
	public static void appendLineToFile(String line, Path path) // #89
			throws UnsupportedEncodingException, FileNotFoundException, IOException {
		File file = new File(path.toString());
		try (FileOutputStream fos = new FileOutputStream(file, true);
				OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
				BufferedWriter writer = new BufferedWriter(osw)) {
			writer.write(line);
			writer.newLine();
		}
	}

	public static BufferedWriter createBufferedWriter(Path path) // #90
			throws UnsupportedEncodingException, FileNotFoundException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toString(), true), "UTF-8"));
		return writer;
	}
	// #91
	
	static void createSymlink(Path link, Path target) throws IOException {
		try {
			Files.createSymbolicLink(link, target);
		} catch (IOException ex) {
			throw new IOException("Can't create symlink " + link + " to target " + target, ex);
		}
	}
	
	static void createDirectories(Path currentRestorePath) throws IOException {
		try {
			Files.createDirectories(currentRestorePath); // #188
		} catch (IOException ex) {
			throw new IOException("Can't create directory " + currentRestorePath, ex);
		}
	}

	public static BufferedReader createBufferedReader(Path path) throws FileNotFoundException, UnsupportedEncodingException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
			    new FileInputStream(path.toString()), "UTF-8"));
		return reader;
	}
	
	public static String readFile(Path path) throws IOException {
	    BufferedReader br = createBufferedReader(path);
	    try {
	        StringBuilder sb = new StringBuilder();
	        String line = br.readLine();

	        while (line != null) {
	            sb.append(line);
	            sb.append("\n");
	            line = br.readLine();
	        }
	        return sb.toString();
	    } finally {
	        br.close();
	    }
	}
}
