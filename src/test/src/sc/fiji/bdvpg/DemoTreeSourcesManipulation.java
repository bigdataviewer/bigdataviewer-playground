/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2022 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg;

import bigwarp.BigWarp;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.util.VersionUtils;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.scijava.services.ui.SourceAndConverterServiceUI;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import static sc.fiji.bdvpg.BigWarpDemo.demo2d;

public class DemoTreeSourcesManipulation {
    public static void main(String... args) {
        // Initializes static SourceService and Display Service
        ImageJ ij = new ImageJ();
        ij.ui().showUI();
        System.out.println("BigWarp version:"+ VersionUtils.getVersion(BigWarp.class));
        demo2d();

        SourceAndConverterServiceUI treeUI = ij.get(SourceAndConverterService.class).getUI();

        DefaultTreeModel model = treeUI.getTreeModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeUI.getTreeModel().getRoot();

        int nChildren = model.getChildCount(root);
        IJ.log("There are "+nChildren+" children in the root node");

        // Easier interface:
        SourceAndConverterServiceUI.Node r = treeUI.getRoot();

        IJ.log("There are "+r.sources().length+" sources in the whole tree.");
        IJ.log("The node "+r+" has "+r.children().size()+" children");
        IJ.log("Their names are:");
        r.children().forEach(n -> IJ.log("- "+n.name()+" | path = "+n.path()));
        IJ.log("nSources = "+r.child("src/test/resources/demoSlice.xml").sources().length);
    }
}
