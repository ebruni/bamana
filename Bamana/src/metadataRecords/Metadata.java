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

import java.util.Objects;

public abstract class Metadata {

	String name, owner, group, permissions, creationTime, lastAccessTime, lastModifiedTime, path;

	public Metadata(String[] metadataArray) {
		unpackMetadataFromArray(metadataArray);
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

	public String getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(String creationTime) {
		this.creationTime = creationTime;
	}

	public String getLastAccessTime() {
		return lastAccessTime;
	}

	public void setLastAccessTime(String lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	public String getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void setLastModifiedTime(String lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
	
	public abstract void unpackMetadataFromArray(String[] metadataArray);
	
	@Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Metadata)) {
            return false;
        }
        Metadata metadata = (Metadata) o;
        return Objects.equals(name, metadata.name) &&
                Objects.equals(owner, metadata.owner) &&
                Objects.equals(group, metadata.group) &&
                Objects.equals(permissions, metadata.permissions);
    }
	
    @Override
    public int hashCode() {
        return Objects.hash(name, owner, group, permissions);
    }

}
