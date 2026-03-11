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

package sc.fiji.bdvpg.scijava;
import org.scijava.plugin.Menu;

import java.lang.annotation.Annotation;

public class BdvPgMenus {

	final public static String L1 = "Plugins";
	final public static String L2 = "BigDataViewer-Playground";
	final public static String RootMenu = L1 + ">" + L2 + ">";

	// Layer 3
	final public static String WorkspaceMenu = "Workspace";
	final public static int WorkspaceW = -10;
	final public static String ImportMenu = "Import";
	final public static int ImportW = -8;
	final public static String DisplayMenu = "Display";
	final public static int DisplayW = -7;
	final public static String DatasetMenu = "Dataset";
	final public static int DatasetW = -9;
	final public static String ProcessMenu = "Process";
	final public static int ProcessW = -6;
	final public static String RegisterMenu = "Register";
	final public static int RegisterW = -5;
	final public static String ExportMenu = "Export";
	final public static int ExportW = -4;
	public static final String BDVMenu = "BDV";
	public static final int BDVW = 0;
	public static final String BVVMenu = "BVV";
	public static final int BVVW = 1;

	static Menu menu(String label, int weight) {
		return new Menu(){

			@Override
			public Class<? extends Annotation> annotationType() {
				return null;
			}

			@Override
			public String label() {
				return label;
			}

			@Override
			public double weight() {
				return weight;
			}

			@Override
			public char mnemonic() {
				return 0;
			}

			@Override
			public String accelerator() {
				return "";
			}

			@Override
			public String iconPath() {
				return "";
			}
		};
	}

}
