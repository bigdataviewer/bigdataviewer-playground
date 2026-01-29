/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2026 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package sc.fiji.bdvpg.scijava.command.spimdata;

import bdv.AbstractSpimSource;
import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for SpimData transform commands.
 *
 * @author Nicolas Chiaruttini, BIOP, EPFL
 */
public class SpimDataTransformUtils {

	private static final Logger logger = LoggerFactory.getLogger(
		SpimDataTransformUtils.class);

	/**
	 * Parses a range specification string into a set of integers.
	 *
	 * Supported formats:
	 * - "5" → {5}
	 * - "0:10" → {0,1,2,...,10}
	 * - "0,3,7" → {0,3,7}
	 * - "0:5,10,15:20" → {0,1,2,3,4,5,10,15,16,17,18,19,20}
	 *
	 * @param rangeSpec the range specification string
	 * @return set of integers
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static Set<Integer> parseRange(String rangeSpec) {
		Set<Integer> result = new HashSet<>();

		if (rangeSpec == null || rangeSpec.trim().isEmpty()) {
			throw new IllegalArgumentException("Range specification cannot be empty");
		}

		String[] parts = rangeSpec.split(",");
		for (String part : parts) {
			part = part.trim();
			if (part.isEmpty()) continue;

			if (part.contains(":")) {
				// Range specification
				String[] rangeParts = part.split(":");
				if (rangeParts.length != 2) {
					throw new IllegalArgumentException(
						"Invalid range format: '" + part + "'. Expected 'min:max'");
				}
				try {
					int min = Integer.parseInt(rangeParts[0].trim());
					int max = Integer.parseInt(rangeParts[1].trim());
					if (min > max) {
						throw new IllegalArgumentException(
							"Invalid range: min (" + min + ") > max (" + max + ")");
					}
					for (int i = min; i <= max; i++) {
						result.add(i);
					}
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
						"Invalid number in range: '" + part + "'");
				}
			} else {
				// Single value
				try {
					result.add(Integer.parseInt(part));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
						"Invalid number: '" + part + "'");
				}
			}
		}

		if (result.isEmpty()) {
			throw new IllegalArgumentException("Range specification produced no values");
		}

		return result;
	}

	/**
	 * Parses a transform matrix string (3 rows, 4 values each) into an AffineTransform3D.
	 *
	 * @param matrixStr the matrix string with 3 lines of 4 comma-separated values
	 * @return the parsed AffineTransform3D
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static AffineTransform3D parseTransformMatrix(String matrixStr) {
		if (matrixStr == null || matrixStr.trim().isEmpty()) {
			throw new IllegalArgumentException("Transform matrix cannot be empty");
		}

		String[] lines = matrixStr.trim().split("\n");
		if (lines.length != 3) {
			throw new IllegalArgumentException(
				"Transform matrix must have exactly 3 rows, got " + lines.length);
		}

		double[][] values = new double[3][4];

		for (int row = 0; row < 3; row++) {
			String[] parts = lines[row].split(",");
			if (parts.length != 4) {
				throw new IllegalArgumentException(
					"Row " + (row + 1) + " must have exactly 4 values, got " + parts.length);
			}
			for (int col = 0; col < 4; col++) {
				try {
					values[row][col] = Double.parseDouble(parts[col].trim());
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(
						"Invalid number at row " + (row + 1) + ", column " + (col + 1) +
							": '" + parts[col].trim() + "'");
				}
			}
		}

		AffineTransform3D transform = new AffineTransform3D();
		transform.set(
			values[0][0], values[0][1], values[0][2], values[0][3],
			values[1][0], values[1][1], values[1][2], values[1][3],
			values[2][0], values[2][1], values[2][2], values[2][3]
		);

		return transform;
	}

	/**
	 * Forces a BDV source to reload its transform from SpimData.
	 * Uses reflection to call the private loadTimepoint method.
	 *
	 * @param sac the source to reload
	 * @param timepoint the timepoint to reload
	 */
	public static void reloadSourceTransform(SourceAndConverter<?> sac, int timepoint) {
		if (!(sac.getSpimSource() instanceof AbstractSpimSource)) {
			return;
		}

		try {
			Method loadTimepoint = Class.forName("bdv.AbstractSpimSource")
				.getDeclaredMethod("loadTimepoint", int.class);
			loadTimepoint.setAccessible(true);

			AbstractSpimSource<?> spimSource = (AbstractSpimSource<?>) sac.getSpimSource();
			loadTimepoint.invoke(spimSource, timepoint);

			// Also reload volatile source if present
			if (sac.asVolatile() != null &&
				sac.asVolatile().getSpimSource() instanceof AbstractSpimSource)
			{
				AbstractSpimSource<?> volatileSource =
					(AbstractSpimSource<?>) sac.asVolatile().getSpimSource();
				loadTimepoint.invoke(volatileSource, timepoint);
			}
		}
		catch (Exception e) {
			logger.warn("Failed to reload timepoint {} for source: {}",
				timepoint, e.getMessage());
		}
	}

	/**
	 * Default identity transform matrix string (3 lines, 4 values each).
	 */
	public static final String IDENTITY_MATRIX = "1, 0, 0, 0\n0, 1, 0, 0\n0, 0, 1, 0";
}
