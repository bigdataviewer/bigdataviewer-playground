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
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import org.scijava.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

public class SourceAndConverterServiceLoader extends SourceAndConverterSerializer implements Runnable{

    String filePath;
    Context ctx;

    public SourceAndConverterServiceLoader(String filePath, String basePath, Context ctx) {
        super(ctx, new File(basePath));
        this.filePath = filePath;
        idToSac = new HashMap<>();
        sacToId = new HashMap<>();
        sourceToId = new HashMap<>();
        idToSource = new HashMap<>();
        this.ctx = ctx;
    }

    @Override
    public void run() {

        // Empty service
        SourceAndConverter[] sacs =
                SourceAndConverterServices
                        .getSourceAndConverterService()
                        .getSourceAndConverters().toArray(new SourceAndConverter[0]);

        SourceAndConverterServices
                .getSourceAndConverterService()
                .remove(sacs);

        try {
            FileReader fileReader = new FileReader(filePath);

            Gson gson = new Gson();
            JsonArray rawSacsArray = gson.fromJson(fileReader, JsonArray.class);
            System.out.println(rawSacsArray.size());

            for (int i = 0;i<rawSacsArray.size();i++) {
                if (rawSacsArray.get(i).isJsonObject()) {
                    idToJsonElement.put(rawSacsArray.get(i).getAsJsonObject().get("source_id").getAsInt(), rawSacsArray.get(i));
                } else {
                    // Source couldn't be serialized
                }
            }

            SourceAndConverter[] sacs_loaded = getGson().fromJson(rawSacsArray, SourceAndConverter[].class);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
