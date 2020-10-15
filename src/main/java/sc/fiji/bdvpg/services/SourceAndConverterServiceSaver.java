/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2020 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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
package sc.fiji.bdvpg.services;

import bdv.viewer.SourceAndConverter;
import com.google.gson.*;
import org.scijava.Context;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;

/** Big Objective : save the state of all open sources
 * By Using Gson and specific serialization depending on SourceAndConverter classes
 *
 * TODO : take care of sources not built with SourceAndConverter
 *
 * TODO : BUG do not work if the same spimdata is opened several times!
 *
 */

public class SourceAndConverterServiceSaver extends SourceAndConverterSerializer implements Runnable {

    File f;

    public SourceAndConverterServiceSaver(File f, Context ctx) {
        super(ctx);
        this.f = f;
        idToSac = new HashMap<>();
        sacToId = new HashMap<>();
        sourceToId = new HashMap<>();
        idToSource = new HashMap<>();
    }

    @Override
    public void run() {
        synchronized (SourceAndConverterServiceSaver.class) {
            List<SourceAndConverter> sacs = SourceAndConverterServices
                    .getSourceAndConverterService()
                    .getSourceAndConverters();

            for (int i = 0; i < sacs.size(); i++) {
                idToSac.put(i, sacs.get(i));
                sacToId.put(sacs.get(i), i);
                idToSource.put(i, sacs.get(i).getSpimSource());
                sourceToId.put(sacs.get(i).getSpimSource(), i);
            }

            Gson gson = getGson();

            try {
                System.out.println(f.getAbsolutePath());
                FileWriter writer = new FileWriter(f.getAbsolutePath());
                gson.toJson(sacs, writer);
                writer.flush();
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
