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

public abstract class ProgressPrinter {

	protected double elements_count, elementsVisited = 0;
	protected long nanosecondsOut, nanosecondsIn;
	
	public ProgressPrinter(double elements_count) {
		this.elements_count = elements_count;
		nanosecondsOut = System.nanoTime();
	}
	
	void printProgress() {
		int percentuale = (int) ((elementsVisited / elements_count) * 100);
		System.out.print("\r[");
		for (int i = 0; i < percentuale / 2; i++) {
			System.out.print("#");
		}
		for (int i = 0; i < 50 - (percentuale / 2); i++) {
			System.out.print(".");
		}
		System.out.print(
				"] " + percentuale + "%   " + (int) elementsVisited + "/" + (int) elements_count + " items processed");
	}

	abstract void elaboratePercentage(boolean last);
	
}
