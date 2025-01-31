/*-
 * #%L
 * BigDataViewer-Playground
 * %%
 * Copyright (C) 2019 - 2025 Nicolas Chiaruttini, EPFL - Robert Haase, MPI CBG - Christian Tischer, EMBL
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

import org.reflections.Reflections;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildDocumentation {
    static String doc = "";
    static final String linkGitHubRepoPrefix = "https://github.com/bigdataviewer/bigdataviewer-playground/tree/master/src/main/java/";

    public static void main(String... args) {
        //

        Reflections reflections = new Reflections("sc.fiji.bdvpg");

        Set<Class<? extends Command>> commandClasses =
                reflections.getSubTypesOf(Command.class);

        HashMap<String, String> docPerClass = new HashMap<>();

        commandClasses.forEach(c -> {

            Plugin plugin = c.getAnnotation(Plugin.class);
            if (plugin!=null) {
                String url = linkGitHubRepoPrefix+c.getName().replaceAll("\\.","\\/")+".java";
                doc = "### [" + c.getSimpleName() + "]("+url+") [" + (plugin.menuPath() == null ? "null" : plugin.menuPath()) + "]\n";
                if (!plugin.label().equals(""))
                    doc+=plugin.label()+"\n";
                if (!plugin.description().equals(""))
                    doc+=plugin.description()+"\n";

                Field[] fields = c.getDeclaredFields();
                List<Field> inputFields = Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.type() == ItemIO.INPUT) || (p.type() == ItemIO.BOTH);
                        }).sorted(Comparator.comparing(Field::getName)).collect(Collectors.toList());
                if (inputFields.size()>0) {
                    doc += "#### Input\n";
                    inputFields.forEach(f -> {
                        doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                        if (!f.getAnnotation(Parameter.class).description().equals(""))
                            doc += f.getAnnotation(Parameter.class).description() + "\n";
                    });
                }

                List<Field> outputFields = Arrays.stream(fields)
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.type() == ItemIO.OUTPUT) || (p.type() == ItemIO.BOTH);
                        }).sorted(Comparator.comparing(Field::getName)).collect(Collectors.toList());
                if (outputFields.size()>0) {
                    doc += "#### Output\n";
                    outputFields.forEach(f -> {
                        doc += "* ["+f.getType().getSimpleName()+"] **" + f.getName() + "**:" + f.getAnnotation(Parameter.class).label() + "\n";
                        if (!f.getAnnotation(Parameter.class).description().equals(""))
                            doc += f.getAnnotation(Parameter.class).description() + "\n";
                    });
                }

                doc+="\n";

                docPerClass.put(c.getName(),doc);
            }
        });
        Object[] keys = docPerClass.keySet().toArray();
        Arrays.sort(keys);
        for (Object key:keys) {
            String k = (String) key;
            System.out.println(docPerClass.get(k));
        }
    }
}
