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

package metadataRecords;

public class DirectoryMetadata extends Metadata {

	public DirectoryMetadata(String[] metadataArray) {
		super(metadataArray);
	}

	@Override
	public void unpackMetadataFromArray(String[] metadataArray) {
		name = metadataArray[0];
		owner = metadataArray[1];
		group = metadataArray[2];
		permissions = metadataArray[3];
		creationTime = metadataArray[4];
		lastAccessTime = metadataArray[5];
		lastModifiedTime = metadataArray[6];
	}
	
}
